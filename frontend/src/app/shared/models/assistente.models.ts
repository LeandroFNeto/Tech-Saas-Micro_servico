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
  modo?: 'DIAGNOSTICO' | 'ATENDIMENTO_SANDBOX';
}

export interface AssistenteChatResponseDTO {
  resposta: string;
  modo: string;
  dataHora: string;
  sugestoes?: string[];
}
