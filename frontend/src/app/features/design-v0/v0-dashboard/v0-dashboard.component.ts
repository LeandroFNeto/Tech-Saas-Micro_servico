import { Component } from '@angular/core';
import contentData from '../../../../../../especificacao/front/v0-dashboard.content.json';
import designData from '../../../../../../especificacao/front/telas.design.json';

@Component({
  selector: 'app-v0-dashboard',
  templateUrl: './v0-dashboard.component.html'
})
export class V0DashboardComponent {
  readonly conteudo = contentData.v0Dashboard;
  readonly design = designData.telas.v0Dashboard;
}
