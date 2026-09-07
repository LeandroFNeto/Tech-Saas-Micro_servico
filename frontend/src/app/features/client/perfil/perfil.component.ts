import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { mensagemHttp } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { AvisoService } from '../../../core/services/aviso.service';
import { EmpresaService } from '../../../core/services/empresa.service';
import { BannerAvisosComponent } from '../../../shared/components/banner-avisos/banner-avisos.component';
import { ConexaoWhatsappComponent } from '../../../shared/components/conexao-whatsapp/conexao-whatsapp.component';
import { AvisoPainel } from '../../../shared/models/empresa.models';

@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, BannerAvisosComponent, ConexaoWhatsappComponent],
  templateUrl: './perfil.component.html'
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly empresasApi = inject(EmpresaService);
  private readonly avisosApi = inject(AvisoService);

  readonly avisos: AvisoPainel[] = this.avisosApi.listar();
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly sessao = this.auth.sessao()?.sessaoWhatsapp ?? '';

  readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    mensagemSaudacao: [''],
    tabelaDePrecos: [''],
    linkGoogleMaps: [''],
    linkFotoPrincipal: [''],
    linkGaleria: ['']
  });

  ngOnInit(): void {
    if (!this.sessao) {
      this.erro.set('Sessão WhatsApp não encontrada neste usuário.');
      return;
    }

    this.empresasApi.buscarPorSessao(this.sessao).subscribe({
      next: (empresa) =>
        this.form.patchValue({
          nome: empresa.nome,
          mensagemSaudacao: empresa.mensagemSaudacao ?? '',
          tabelaDePrecos: empresa.tabelaDePrecos ?? '',
          linkGoogleMaps: empresa.linkGoogleMaps ?? '',
          linkFotoPrincipal: empresa.linkFotoPrincipal ?? '',
          linkGaleria: empresa.linkGaleria ?? ''
        }),
      error: (err) => this.erro.set(mensagemHttp(err))
    });
  }

  salvar(): void {
    this.erro.set('');
    this.sucesso.set('');
    if (this.form.invalid || !this.sessao) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando.set(true);
    this.empresasApi.atualizarPeloCliente(this.sessao, this.form.getRawValue()).subscribe({
      next: () => {
        this.sucesso.set('Perfil atualizado. O bot já pode usar a nova saudação e os links.');
        this.salvando.set(false);
      },
      error: (err) => {
        this.erro.set(mensagemHttp(err));
        this.salvando.set(false);
      }
    });
  }
}
