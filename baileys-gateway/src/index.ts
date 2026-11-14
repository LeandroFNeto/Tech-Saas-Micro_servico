import { carregarConfig, ErroDeConfiguracao } from './config.js';
import { usarAutenticacaoAtomica } from './auth-state.js';
import { criarFabricaBaileys } from './fabrica-baileys.js';
import { GerenciadorSessoes } from './gerenciador.js';
import { criarLogger } from './logger.js';
import { criarApp } from './rotas.js';
import { DespachanteWebhook } from './webhook.js';

async function iniciar(): Promise<void> {
  let config;
  try {
    config = carregarConfig();
  } catch (erro) {
    const mensagem = erro instanceof ErroDeConfiguracao ? erro.message : 'configuracao invalida';
    process.stderr.write(`${mensagem}\n`);
    process.exit(1);
  }

  const log = criarLogger(config.nivelLog);
  const despachante = new DespachanteWebhook({ segredo: config.segredoWebhook }, log);
  const gerenciador = new GerenciadorSessoes({
    config,
    log,
    fabrica: criarFabricaBaileys(config, log),
    carregarAutenticacao: usarAutenticacaoAtomica,
    despachante
  });

  const app = criarApp({ config, log, gerenciador });
  const servidor = app.listen(config.porta, () => {
    log.info({ porta: config.porta }, 'baileys-gateway no ar');
  });

  await gerenciador.restaurar();

  const encerrar = (sinal: string): void => {
    log.info({ sinal }, 'Encerrando o gateway');
    servidor.close();
    void gerenciador.parar().finally(() => process.exit(0));
  };
  process.on('SIGTERM', () => encerrar('SIGTERM'));
  process.on('SIGINT', () => encerrar('SIGINT'));
  process.on('unhandledRejection', (erro) => {
    log.error({ erro }, 'Promessa rejeitada sem tratamento');
  });
}

void iniciar();
