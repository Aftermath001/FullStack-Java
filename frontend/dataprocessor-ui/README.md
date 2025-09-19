# Data Processor UI

Angular frontend application for the Data Processor backend service.

## Features

- **Generator**: Create Excel files with student data
- **Converter**: Convert Excel files to CSV format
- **Uploader**: Upload CSV files to import student data
- **Reports**: View, filter, and export student data

## Development

### Prerequisites
- Node.js (v18 or higher)
- npm

### Installation
```bash
cd frontend/dataprocessor-ui
npm install
```

### Running the Application
```bash
npm start
```

The application will be available at `http://localhost:4200`

### Backend Configuration
Make sure the Spring Boot backend is running on `http://localhost:8080` before using the frontend.

## Components

### Generator Component
- Form to input number of records to generate
- Generates Excel files with student data
- Downloads generated files automatically

### Converter Component
- File upload for Excel (.xlsx) files
- Converts Excel to CSV format
- Downloads converted CSV files

### Uploader Component
- File upload for CSV files
- Imports student data to database
- Validates file format and provides feedback

### Report Component
- Data table with pagination
- Filtering by Student ID, Class, and Name search
- Export functionality (CSV, Excel, PDF)
- Real-time data updates

## Technologies Used

- Angular 18
- Angular Material
- RxJS
- TypeScript
- SCSS