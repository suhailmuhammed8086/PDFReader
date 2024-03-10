package com.example.pdfnotemate.di

import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.repository.PdfRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPdfRepository(impl: PdfRepositoryImpl): PDFRepository
}