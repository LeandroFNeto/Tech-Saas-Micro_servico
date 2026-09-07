import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AvisoService } from '../../../core/services/aviso.service';
import { AvisoDTO, TIPOS_AVISO, TipoAviso, rotuloTipoAviso } from '../../../shared/models/aviso.models';

@Component({
  selector: 'app-gestao-avisos',
  imports: [ReactiveFormsModule],
  templateUrl: './gestao-avisos.component.html'
})
export class GestaoAvisosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly avisosApi = inject(AvisoService);

  readonly tipos = TIPOS_AVISO;
  readonly avisos = signal<AvisoDTO[]>([]);
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly editandoId = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    titulo: ['', Validators.required],
    tipo: this.fb.nonNullable.control<TipoAviso>('COMUNICADO', Validators.required),
    conteudo: ['', Validators.required],
    link: [''],
    ativo: [true]
  });

  ngOnInit(): void {
    this.carregar();
  }

  rotulo(tipo: TipoAviso): string {
    return rotuloTipoAviso(tipo);
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    this.avisosApi.listarAdmin().subscribe({
      next: (lista) => {
        this.avisos.set(lista);
        this.carregando.set(false);
      },
      error: (err) => {
        this.erro.set(mensagemHttp(err));
        this.carregando.set(false);
      }
    });
  }

  editar(aviso: AvisoDTO): void {
    this.editandoId.set(aviso.id ?? null);
    this.sucesso.set('');
    this.form.patchValue({
      titulo: aviso.titulo,
      tipo: aviso.tipo,
      conteudo: aviso.conteudo,
      link: aviso.link ?? '',
      ativo: aviso.ativo
    });
  }

  cancelarEdicao(): void {
    this.editandoId.set(null);
    this.form.reset({
      titulo: '',
      tipo: 'COMUNICADO',
      conteudo: '',
      link: '',
      ativo: true
    });
  }

  salvar(): void {
    this.erro.set('');
    this.sucesso.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const dto: AvisoDTO = {
      tipo: valor.tipo,
      titulo: valor.titulo,
      conteudo: valor.conteudo,
      link: valor.link.trim() || null,
      ativo: valor.ativo
    };

    this.salvando.set(true);
    const id = this.editandoId();
    const pedido = id == null ? this.avisosApi.criar(dto) : this.avisosApi.atualizar(id, dto);

    pedido.subscribe({
      next: () => {
        this.sucesso.set(id == null ? 'Aviso criado.' : 'Aviso atualizado.');
        this.salvando.set(false);
        this.cancelarEdicao();
        this.carregar();
      },
      error: (err) => {
        this.erro.set(mensagemHttp(err));
        this.salvando.set(false);
      }
    });
  }

  excluir(aviso: AvisoDTO): void {
    if (aviso.id == null) {
      return;
    }
    if (!confirm(`Excluir o aviso "${aviso.titulo}"?`)) {
      return;
    }

    this.erro.set('');
    this.avisosApi.excluir(aviso.id).subscribe({
      next: () => {
        if (this.editandoId() === aviso.id) {
          this.cancelarEdicao();
        }
        this.sucesso.set('Aviso excluído.');
        this.carregar();
      },
      error: (err) => this.erro.set(mensagemHttp(err))
    });
  }
}
