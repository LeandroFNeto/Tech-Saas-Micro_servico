import express, { type NextFunction, type Request, type Response } from 'express';
import type { Logger } from 'pino';
import type { Config } from './config.js';
import { webhookPermitido } from './config.js';
import type { GerenciadorSessoes } from './gerenciador.js';
import { montarDestino } from './jid.js';
import { baixarImagem, ErroDeMidia, type OpcoesDownload } from './midia.js';
import { mascararNumero, tamanhoTexto } from './mascara.js';
import { bytesDoPng } from './qr.js';
import { extrairBearer, gerarToken, igualSeguro, sessaoValida, tokenValido } from './seguranca.js';
import { ErroDeNegocio } from './sessao.js';

export interface DependenciasRotas {
  config: Config;
  log: Logger;
  gerenciador: GerenciadorSessoes;
  opcoesDownload?: Partial<OpcoesDownload>;
}

const LIMITE_TEXTO = 10000;
const LIMITE_LEGENDA = 4096;

function booleano(valor: unknown): boolean {
  return valor === true || valor === 'true';
}

export function criarApp(deps: DependenciasRotas): express.Express {
  const { config, log, gerenciador } = deps;
  const app = express();
  app.disable('x-powered-by');
  app.use(express.json({ limit: '8mb' }));

  const exigirSessaoValida = (req: Request, res: Response, next: NextFunction): void => {
    if (!sessaoValida(String(req.params.session ?? ''))) {
      res.status(400).json({ status: 'error', message: 'Sessao invalida' });
      return;
    }
    next();
  };

  const exigirToken = (req: Request, res: Response, next: NextFunction): void => {
    const token = extrairBearer(req.header('authorization'));
    if (!tokenValido(config.segredo, String(req.params.session), token)) {
      res.status(401).json({ status: 'error', message: 'Token invalido' });
      return;
    }
    next();
  };

  app.get('/api/health', (req, res) => {
    const autorizacao = String(req.headers.authorization ?? '');
    const enviado = autorizacao.startsWith('Bearer ') ? autorizacao.slice(7) : '';
    if (!igualSeguro(enviado, config.segredo)) {
      res.status(401).json({ status: 'error', message: 'Nao autorizado' });
      return;
    }
    res.json({ status: 'ok', sessoes: gerenciador.saude() });
  });

  app.post('/api/:session/:secret/generate-token', exigirSessaoValida, (req, res) => {
    if (!igualSeguro(String(req.params.secret), config.segredo)) {
      res.status(401).json({ status: 'error', message: 'Segredo invalido' });
      return;
    }
    const sessao = String(req.params.session);
    res.json({ status: 'success', session: sessao, token: gerarToken(config.segredo, sessao) });
  });

  app.post('/api/:session/start-session', exigirSessaoValida, exigirToken, async (req, res, next) => {
    try {
      const sessao = String(req.params.session);
      const webhook = typeof req.body?.webhook === 'string' ? req.body.webhook : undefined;
      if (webhook !== undefined && !webhookPermitido(webhook, config.hostsWebhookPermitidos)) {
        res.status(400).json({ status: 'error', message: 'Webhook invalido' });
        return;
      }
      const instancia = await gerenciador.iniciar(sessao, webhook);
      res.json({ ...instancia.status() });
    } catch (erro) {
      next(erro);
    }
  });

  app.get('/api/:session/status-session', exigirSessaoValida, exigirToken, (req, res) => {
    const instancia = gerenciador.obter(String(req.params.session));
    res.json(instancia ? instancia.status() : { status: 'DISCONNECTED', qrcode: null });
  });

  app.get('/api/:session/qrcode-session', exigirSessaoValida, exigirToken, (req, res) => {
    const qr = gerenciador.obter(String(req.params.session))?.qrAtual();
    if (!qr) {
      res.json({ status: null, qrcode: null });
      return;
    }
    res.type('image/png').send(bytesDoPng(qr));
  });

  const preparar = async (
    req: Request,
    res: Response
  ): Promise<{ instancia: NonNullable<ReturnType<GerenciadorSessoes['obter']>>; jid: string } | null> => {
    const instancia = gerenciador.obter(String(req.params.session));
    if (!instancia || !instancia.conectada) {
      res.status(404).json({ status: 'error', message: 'Sessao desconectada' });
      return null;
    }
    const destino = montarDestino(String(req.body?.phone ?? ''), booleano(req.body?.isLid));
    if (!destino) {
      res.status(400).json({ status: 'error', message: 'Numero invalido' });
      return null;
    }
    if (booleano(req.body?.isGroup)) {
      res.status(400).json({ status: 'error', message: 'Envio para grupo nao suportado' });
      return null;
    }
    if (destino.tipo === 'lid') {
      return { instancia, jid: destino.jid };
    }
    let jid: string | null;
    try {
      jid = await instancia.resolverDestinoDeNumero(destino.digitos);
    } catch (erro) {
      log.warn({ sessao: instancia.nome, erro }, 'Falha ao conferir o numero no WhatsApp');
      res.status(instancia.conectada ? 502 : 404).json({ status: 'error', message: 'Falha ao conferir o numero' });
      return null;
    }
    if (!jid) {
      res.status(400).json({ status: 'error', message: 'Numero sem WhatsApp' });
      return null;
    }
    return { instancia, jid };
  };

  app.post('/api/:session/send-message', exigirSessaoValida, exigirToken, async (req, res, next) => {
    try {
      const texto = req.body?.message;
      if (typeof texto !== 'string' || texto.trim() === '' || texto.length > LIMITE_TEXTO) {
        res.status(400).json({ status: 'error', message: 'Mensagem invalida' });
        return;
      }
      const pronto = await preparar(req, res);
      if (!pronto) {
        return;
      }
      if (!pronto.instancia.enfileirarTexto(pronto.jid, texto)) {
        res.status(503).json({ status: 'error', message: 'Fila de envio cheia' });
        return;
      }
      log.info({ sessao: pronto.instancia.nome, numero: mascararNumero(pronto.jid), texto: tamanhoTexto(texto) }, 'Envio de texto aceito');
      res.json({ status: 'success' });
    } catch (erro) {
      next(erro);
    }
  });

  app.post('/api/:session/send-image', exigirSessaoValida, exigirToken, async (req, res, next) => {
    try {
      const origem = req.body?.path;
      const legenda = typeof req.body?.caption === 'string' ? req.body.caption : undefined;
      if (typeof origem !== 'string' || origem === '' || (legenda !== undefined && legenda.length > LIMITE_LEGENDA)) {
        res.status(400).json({ status: 'error', message: 'Imagem invalida' });
        return;
      }
      const pronto = await preparar(req, res);
      if (!pronto) {
        return;
      }
      let imagem;
      try {
        imagem = await baixarImagem(origem, {
          limiteBytes: config.limiteImagemBytes,
          timeoutMs: config.timeoutDownloadMs,
          hostsPermitidos: config.hostsFotoPermitidos,
          ...deps.opcoesDownload
        });
      } catch (erro) {
        const status = erro instanceof ErroDeMidia ? erro.status : 502;
        log.warn({ sessao: pronto.instancia.nome, motivo: erro instanceof Error ? erro.message : 'erro' }, 'Imagem recusada');
        res.status(status).json({ status: 'error', message: 'Imagem indisponivel' });
        return;
      }
      if (!pronto.instancia.enfileirarImagem(pronto.jid, imagem.buffer, imagem.mimetype, legenda)) {
        res.status(503).json({ status: 'error', message: 'Fila de envio cheia' });
        return;
      }
      log.info({ sessao: pronto.instancia.nome, numero: mascararNumero(pronto.jid), bytes: imagem.buffer.length }, 'Envio de imagem aceito');
      res.json({ status: 'success' });
    } catch (erro) {
      next(erro);
    }
  });

  app.post('/api/:session/close-session', exigirSessaoValida, exigirToken, async (req, res, next) => {
    try {
      await gerenciador.fechar(String(req.params.session));
      log.info({ sessao: String(req.params.session) }, 'Sessao encerrada e credenciais apagadas');
      res.json({ status: true, message: 'Sessao encerrada' });
    } catch (erro) {
      next(erro);
    }
  });

  app.post('/api/:session/request-pairing-code', exigirSessaoValida, exigirToken, async (req, res, next) => {
    try {
      const telefone = String(req.body?.phone ?? '');
      if (!/^\d{10,15}$/.test(telefone)) {
        res.status(400).json({ status: 'error', message: 'Telefone invalido' });
        return;
      }
      const instancia = gerenciador.obter(String(req.params.session));
      if (!instancia) {
        res.status(409).json({ status: 'error', message: 'Sessao nao esta aguardando pareamento' });
        return;
      }
      const codigo = await instancia.solicitarCodigoPareamento(telefone);
      log.info({ sessao: instancia.nome, numero: mascararNumero(telefone) }, 'Codigo de pareamento gerado');
      res.json({ code: codigo });
    } catch (erro) {
      next(erro);
    }
  });

  app.use((_req, res) => {
    res.status(404).json({ status: 'error', message: 'Rota nao encontrada' });
  });

  app.use((erro: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (erro instanceof ErroDeNegocio) {
      res.status(erro.status).json({ status: 'error', message: erro.message });
      return;
    }
    const tipo = (erro as { type?: string } | undefined)?.type;
    if (tipo === 'entity.parse.failed' || tipo === 'entity.too.large') {
      res.status(400).json({ status: 'error', message: 'Corpo invalido' });
      return;
    }
    log.error({ erro }, 'Erro nao tratado na API');
    res.status(500).json({ status: 'error', message: 'Erro interno' });
  });

  return app;
}
