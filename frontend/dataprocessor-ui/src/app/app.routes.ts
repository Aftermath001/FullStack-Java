import { Routes } from '@angular/router';
import { GeneratorComponent } from './components/generator/generator.component';
import { ConverterComponent } from './components/converter/converter.component';
import { UploaderComponent } from './components/uploader/uploader.component';
import { ReportComponent } from './components/report/report.component';

export const routes: Routes = [
  { path: '', redirectTo: '/generator', pathMatch: 'full' },
  { path: 'generator', component: GeneratorComponent },
  { path: 'converter', component: ConverterComponent },
  { path: 'uploader', component: UploaderComponent },
  { path: 'report', component: ReportComponent },
  { path: '**', redirectTo: '/generator' }
];