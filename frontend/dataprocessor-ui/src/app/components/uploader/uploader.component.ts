import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-uploader',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './uploader.component.html',
  styleUrls: ['./uploader.component.scss']
})
export class UploaderComponent {
  selectedFile: File | null = null;
  isLoading: boolean = false;

  constructor(
    private dataService: DataService,
    private snackBar: MatSnackBar
  ) {}

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (file.type === 'text/csv' || file.name.endsWith('.csv')) {
        this.selectedFile = file;
        this.snackBar.open(`Selected file: ${file.name}`, 'Close', {
          duration: 2000
        });
      } else {
        this.snackBar.open('Please select a valid CSV file', 'Close', {
          duration: 3000
        });
        this.selectedFile = null;
        event.target.value = '';
      }
    }
  }

  uploadCsv(): void {
    if (!this.selectedFile) {
      this.snackBar.open('Please select a CSV file first', 'Close', {
        duration: 3000
      });
      return;
    }

    this.isLoading = true;
    this.dataService.uploadCsv(this.selectedFile).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.snackBar.open('CSV file uploaded and imported successfully!', 'Close', {
          duration: 5000
        });
        console.log('Upload response:', response);
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Error uploading CSV file', 'Close', {
          duration: 3000
        });
        console.error('Error:', error);
      }
    });
  }
}