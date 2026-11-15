const DIGITOS_MAXIMOS = 13;

export function somenteDigitos(texto: string): string {
  return (texto ?? '').replace(/\D/g, '').slice(0, DIGITOS_MAXIMOS);
}

export function telefoneValido(digitos: string): boolean {
  return /^55\d{10,11}$/.test(digitos);
}

export function formatarTelefone(digitos: string): string {
  if (!digitos) {
    return '';
  }
  const ddd = digitos.slice(2, 4);
  const resto = digitos.slice(4);
  let texto = `+${digitos.slice(0, 2)}`;
  if (ddd) {
    texto += ` (${ddd}${ddd.length === 2 ? ')' : ''}`;
  }
  if (resto.length > 5) {
    texto += ` ${resto.slice(0, 5)}-${resto.slice(5)}`;
  } else if (resto) {
    texto += ` ${resto}`;
  }
  return texto;
}

export function formatarTempo(segundos: number): string {
  const total = Math.max(0, Math.floor(segundos));
  const minutos = Math.floor(total / 60);
  return `${minutos}:${String(total % 60).padStart(2, '0')}`;
}
