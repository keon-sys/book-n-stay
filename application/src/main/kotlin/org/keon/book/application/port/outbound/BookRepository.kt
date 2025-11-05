package org.keon.book.application.port.outbound

interface BookRepository {
    fun fetchBooks()
    fun fetchBook()
}