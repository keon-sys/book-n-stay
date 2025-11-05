package org.keon.book.application.port.inbound

interface BookUseCase {
    fun getBooks(query: BooksQuery)

    data class BooksQuery(
        val id: String,
    )

    data class BookCommand(
        val id: String,
    )

    data class BookResponse(
        val data: String,
    )
}