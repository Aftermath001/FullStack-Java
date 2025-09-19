import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { DataService } from '../../services/data.service';
import { Student } from '../../models/student.model';
import { Page } from '../../models/page.model';

@Component({
  selector: 'app-report',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule
  ],
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.scss']
})
export class ReportComponent implements OnInit {
  students: Student[] = [];
  displayedColumns: string[] = ['studentId', 'firstName', 'lastName', 'dob', 'clazz', 'score'];
  
  // Pagination
  totalElements: number = 0;
  pageSize: number = 20;
  pageIndex: number = 0;
  pageSizeOptions: number[] = [10, 20, 50, 100];
  
  // Filters
  studentIdFilter: string = '';
  clazzFilter: string = '';
  searchFilter: string = '';
  
  // Classes for dropdown
  classes: string[] = ['Class1', 'Class2', 'Class3', 'Class4', 'Class5'];
  
  isLoading: boolean = false;
  isExporting: boolean = false;

  constructor(
    private dataService: DataService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadStudents();
  }

  loadStudents(): void {
    this.isLoading = true;
    
    const studentId = this.studentIdFilter ? parseInt(this.studentIdFilter) : undefined;
    const clazz = this.clazzFilter || undefined;
    const search = this.searchFilter || undefined;
    
    this.dataService.getStudents(this.pageIndex, this.pageSize, studentId, clazz, search)
      .subscribe({
        next: (page: Page<Student>) => {
          this.students = page.content;
          this.totalElements = page.totalElements;
          this.isLoading = false;
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open('Error loading students', 'Close', {
            duration: 3000
          });
          console.error('Error:', error);
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadStudents();
  }

  applyFilters(): void {
    this.pageIndex = 0; // Reset to first page when filtering
    this.loadStudents();
  }

  clearFilters(): void {
    this.studentIdFilter = '';
    this.clazzFilter = '';
    this.searchFilter = '';
    this.pageIndex = 0;
    this.loadStudents();
  }

  exportData(format: string): void {
    this.isExporting = true;
    
    const studentId = this.studentIdFilter ? parseInt(this.studentIdFilter) : undefined;
    const clazz = this.clazzFilter || undefined;
    const search = this.searchFilter || undefined;
    
    this.dataService.exportStudents(format, this.pageIndex, this.pageSize, studentId, clazz, search)
      .subscribe({
        next: (blob) => {
          this.isExporting = false;
          this.downloadBlob(blob, `students_export.${format}`);
          this.snackBar.open(`${format.toUpperCase()} export completed!`, 'Close', {
            duration: 3000
          });
        },
        error: (error) => {
          this.isExporting = false;
          this.snackBar.open(`Error exporting ${format.toUpperCase()}`, 'Close', {
            duration: 3000
          });
          console.error('Export error:', error);
        }
      });
  }

  private downloadBlob(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}