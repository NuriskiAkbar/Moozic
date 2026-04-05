package id.rhiquest.mozzic.Repository.Local

interface TransactionRepository {
    suspend fun addToFavorites(id: String)
}