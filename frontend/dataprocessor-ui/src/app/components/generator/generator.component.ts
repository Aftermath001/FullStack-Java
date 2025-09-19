import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-generator',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './generator.component.html',
  styleUrls: ['./generator.component.scss']
})
export class GeneratorComponent {
  count: number = 1000;
  isLoading: boolean = false;

  constructor(
    private dataService: DataService,
    private snackBar: MatSnackBar
  ) {}

  generateExcel(): void {
    if (this.count <= 0) {
      this.snackBar.open('Please enter a valid count greater than 0', 'Close', {
        duration: 3000
      });
      return;
    }

    this.isLoading = true;
    this.dataService.generateExcel(this.count).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.snackBar.open(`Excel file generated successfully!`, 'Download', {
          duration: 5000
        }).onAction().subscribe(() => {
          this.downloadFile(response.downloadLink);
        });
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Error generating Excel file', 'Close', {
          duration: 3000
        });
        console.error('Error:', error);
      }
    });
  }

  private downloadFile(downloadLink: string): void {
    const fileName = downloadLink.split('/').pop() || 'generated_file.xlsx';
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