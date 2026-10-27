export const FONTE_MINIMA_PX = 16;

const TIPOS_IGNORADOS = ['checkbox', 'radio', 'hidden', 'file'];

export interface CampoFonte {
  descricao: string;
  tamanhoPx: number;
}

function descrever(campo: Element): string {
  const tipo = campo.getAttribute('type');
  const nome = campo.getAttribute('formcontrolname') ?? campo.getAttribute('id') ?? campo.getAttribute('name') ?? '';
  return `${campo.tagName.toLowerCase()}${tipo ? `[type=${tipo}]` : ''}${nome ? `#${nome}` : ''}`;
}

export function medirCampos(raiz: HTMLElement): CampoFonte[] {
  return Array.from(raiz.querySelectorAll<HTMLElement>('input, select, textarea'))
    .filter((campo) => !TIPOS_IGNORADOS.includes((campo.getAttribute('type') ?? '').toLowerCase()))
    .map((campo) => ({
      descricao: descrever(campo),
      tamanhoPx: parseFloat(getComputedStyle(campo).fontSize)
    }));
}

export function camposAbaixoDoMinimo(raiz: HTMLElement): string[] {
  return medirCampos(raiz)
    .filter((campo) => !(campo.tamanhoPx >= FONTE_MINIMA_PX))
    .map((campo) => `${campo.descricao}: ${campo.tamanhoPx}px`);
}

export function garantirTailwindCarregado(raiz: HTMLElement): void {
  const sonda = document.createElement('span');
  sonda.className = 'text-xs';
  raiz.appendChild(sonda);
  const tamanho = parseFloat(getComputedStyle(sonda).fontSize);
  sonda.remove();
  if (tamanho !== 12) {
    throw new Error(`styles.css com Tailwind nao esta ativo no teste (text-xs = ${tamanho}px)`);
  }
}
