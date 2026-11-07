import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ContatoService } from '../../../core/services/contato.service';
import { ContatoResumoDTO } from '../../../shared/models/contato.models';

@Component({
  selector: 'app-contatos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './contatos.component.html'
})
export class ContatosComponent implements OnInit {
  private readonly contatoService = inject(ContatoService);

  readonly contatos = signal<ContatoResumoDTO[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly mensagemSucesso = signal<string | null>(null);
  readonly paginaAtual = signal(0);
  readonly tamanhoPagina = 10;

  readonly numerosRevelados = signal<Record<number, string>>({});
  readonly revelando = signal<Record<number, boolean>>({});
  readonly excluindo = signal<Record<number, boolean>>({});

  ngOnInit(): void {
    this.carregarContatos();
  }

  carregarContatos(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.contatoService.listar(this.paginaAtual(), this.tamanhoPagina).subscribe({
      next: (lista) => {
        this.contatos.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar a lista de contatos.');
        this.carregando.set(false);
      }
    });
  }

  avancarPagina(): void {
    this.paginaAtual.update((p) => p + 1);
    this.carregarContatos();
  }

  voltarPagina(): void {
    if (this.paginaAtual() > 0) {
      this.paginaAtual.update((p) => p - 1);
      this.carregarContatos();
    }
  }

  revelarNumero(contato: ContatoResumoDTO): void {
    this.revelando.update((m) => ({ ...m, [contato.id]: true }));
    this.erro.set(null);

    this.contatoService.revelarNumero(contato.id).subscribe({
      next: (resp) => {
        this.numerosRevelados.update((m) => ({ ...m, [contato.id]: resp.numero }));
        this.revelando.update((m) => ({ ...m, [contato.id]: false }));
      },
      error: (err) => {
        this.revelando.update((m) => ({ ...m, [contato.id]: false }));
        if (err.status === 429) {
          this.erro.set('Limite de 10 contatos por hora atingido para proteção contra vazamento em massa (LGPD). Tente novamente na próxima hora.');
        } else {
          this.erro.set('Erro ao buscar o número completo do contato.');
        }
      }
    });
  }

  excluir(contato: ContatoResumoDTO): void {
    if (!confirm(`Deseja realmente excluir permanentemente os dados do contato "${contato.nome}" em conformidade com o direito ao esquecimento (LGPD)?`)) {
      return;
    }

    this.excluindo.update((m) => ({ ...m, [contato.id]: true }));
    this.contatoService.excluir(contato.id).subscribe({
      next: () => {
        this.excluindo.update((m) => ({ ...m, [contato.id]: false }));
        this.mensagemSucesso.set('Contato removido definitivamente.');
        setTimeout(() => this.mensagemSucesso.set(null), 4000);
        this.carregarContatos();
      },
      error: () => {
        this.excluindo.update((m) => ({ ...m, [contato.id]: false }));
        this.erro.set('Erro ao solicitar a exclusão do contato.');
      }
    });
  }
}
