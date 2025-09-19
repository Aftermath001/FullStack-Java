import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/page.model';
import { Student } from '../models/student.model';

@Injectable({
  providedIn: 'root'
})
export class DataService {
  private baseUrl = '/api';

  constructor(private http: HttpClient) { }

  // Generate Excel file
  generateExcel(count: number): Observable<any> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.post(`${this.baseUrl}/generate`, null, { params });
  }

  // Convert Excel to CSV
  convertExcelToCsv(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.baseUrl}/convert`, formData);
  }

  // Upload CSV to database
  uploadCsv(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.baseUrl}/upload-csv`, formData);
  }

  // Get students with pagination and filters
  getStudents(page: number = 0, size: number = 20, studentId?: number, clazz?: string, search?: string): Observable<Page<Student>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (studentId) {
      params = params.set('studentId', studentId.toString());
    }
    if (clazz) {
      params = params.set('clazz', clazz);
    }
    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<Page<Student>>(`${this.baseUrl}/students`, { params });
  }

  // Export students
  exportStudents(format: string, page: number = 0, size: number = 100, studentId?: number, clazz?: string, search?: string): Observable<Blob> {
    let params = new HttpParams()
      .set('format', format)
      .set('page', page.toString())
      .set('size', size.toString());

    if (studentId) {
      params = params.set('studentId', studentId.toString());
    }
    if (clazz) {
      params = params.set('clazz', clazz);
    }
    if (search) {
      params = params.set('search', search);
    }

    return this.http.get(`${this.baseUrl}/students/export`, { 
      params, 
      responseType: 'blob' 
    });
  }

  // Download file by filename
  downloadFile(fileName: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/download/${fileName}`, { 
      responseType: 'blob' 
    });
  }
}