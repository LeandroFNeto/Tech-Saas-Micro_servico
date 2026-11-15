import { formatarTelefone, formatarTempo, somenteDigitos, telefoneValido } from './conexao-whatsapp.telefone';

describe('conexao-whatsapp.telefone', () => {
  describe('somenteDigitos', () => {
    it('remove símbolos, espaços e letras', () => {
      expect(somenteDigitos('+55 (11) 99999-9999')).toBe('5511999999999');
      expect(somenteDigitos('abc 55-11')).toBe('5511');
    });

    it('limita a 13 dígitos', () => {
      expect(somenteDigitos('55119999999991234')).toBe('5511999999999');
    });

    it('texto vazio ou indefinido vira vazio', () => {
      expect(somenteDigitos('')).toBe('');
      expect(somenteDigitos(undefined as unknown as string)).toBe('');
    });
  });

  describe('telefoneValido', () => {
    it('aceita 12 e 13 dígitos começando com 55', () => {
      expect(telefoneValido('551133334444')).toBeTrue();
      expect(telefoneValido('5511900000000')).toBeTrue();
    });

    it('recusa tamanho errado, sem 55 e vazio', () => {
      expect(telefoneValido('55119000000')).toBeFalse();
      expect(telefoneValido('55119000000000')).toBeFalse();
      expect(telefoneValido('1190000000000')).toBeFalse();
      expect(telefoneValido('11900000000000')).toBeFalse();
      expect(telefoneValido('')).toBeFalse();
    });
  });

  describe('formatarTelefone', () => {
    it('vazio fica vazio', () => {
      expect(formatarTelefone('')).toBe('');
    });

    it('formata progressivamente', () => {
      expect(formatarTelefone('5')).toBe('+5');
      expect(formatarTelefone('55')).toBe('+55');
      expect(formatarTelefone('551')).toBe('+55 (1');
      expect(formatarTelefone('5511')).toBe('+55 (11)');
      expect(formatarTelefone('55119')).toBe('+55 (11) 9');
      expect(formatarTelefone('5511900')).toBe('+55 (11) 900');
      expect(formatarTelefone('551190000')).toBe('+55 (11) 90000');
      expect(formatarTelefone('5511900000')).toBe('+55 (11) 90000-0');
      expect(formatarTelefone('5511900000000')).toBe('+55 (11) 90000-0000');
    });
  });

  describe('formatarTempo', () => {
    it('formata minutos e segundos', () => {
      expect(formatarTempo(120)).toBe('2:00');
      expect(formatarTempo(59)).toBe('0:59');
      expect(formatarTempo(65)).toBe('1:05');
    });

    it('nunca fica negativo', () => {
      expect(formatarTempo(-3)).toBe('0:00');
      expect(formatarTempo(0)).toBe('0:00');
    });
  });
});
