export interface DiagnosticoStatusDTO {
  modulo: string;
  status: 'OPERACIONAL' | 'DEGRADADO' | 'INDISPONIVEL';
  mensagem: string;
  tempoRespostaMs: number;
}

export interface DiagnosticoResponseDTO {
  statusGeral: 'OPERACIONAL' | 'ATENCAO' | 'CRITICO';
  dataHora: string;
  modulos: DiagnosticoStatusDTO[];
}

export interface AssistenteChatRequestDTO {
  mensagem: string;
  modo?: 'DIAGNOSTICO' | 'ATENDIMENTO_SANDBOX' | 'SUPORTE' | 'TESTE_VIRTUAL';
}

export interface SuportePublicoRequestDTO {
  mensagem: string;
}

export interface AssistenteChatResponseDTO {
  resposta: string;
  modo: string;
  dataHora: string;
  sugestoes?: string[];
}
