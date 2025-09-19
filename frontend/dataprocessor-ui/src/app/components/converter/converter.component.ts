import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-converter',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './converter.component.html',
  styleUrls: ['./converter.component.scss']
})
export class ConverterComponent {
  selectedFile: File | null = null;
  isLoading: boolean = false;

  constructor(
    private dataService: DataService,
    private snackBar: MatSnackBar
  ) {}

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
          file.name.endsWith('.xlsx')) {
        this.selectedFile = file;
        this.snackBar.open(`Selected file: ${file.name}`, 'Close', {
          duration: 2000
        });
      } else {
        this.snackBar.open('Please select a valid Excel (.xlsx) file', 'Close', {
          duration: 3000
        });
        this.selectedFile = null;
        event.target.value = '';
      }
    }
  }

  convertToCsv(): void {
    if (!this.selectedFile) {
      this.snackBar.open('Please select an Excel file first', 'Close', {
        duration: 3000
      });
      return;
    }

    this.isLoading = true;
    this.dataService.convertExcelToCsv(this.selectedFile).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.snackBar.open(`Excel file converted to CSV successfully!`, 'Download', {
          duration: 5000
        }).onAction().subscribe(() => {
          this.downloadFile(response.downloadLink);
        });
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Error converting Excel to CSV', 'Close', {
          duration: 3000
        });
        console.error('Error:', error);
      }
    });
  }

  private downloadFile(downloadLink: string): void {
    const fileName = downloadLink.split('/').pop() || 'converted_file.csv';
    this.dataService.downloadFile(fileName).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        this.snackBar.open('Error downloading file', 'Close', {
          duration: 3000
        });
        console.error('Download error:', error);
      }
    });
  }
}