package com.csamanez.firebaseauthentication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import com.csamanez.firebaseauthentication.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration

    private var productSelected: Product? = null

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val response = IdpResponse.fromResultIntent(it.data)

            if (it.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Welcome...", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, "See you later...", Toast.LENGTH_SHORT).show()
                    finish() // Finaliza el app
                } else {
                    response.error?.let {
                        if (it.errorCode == ErrorCodes.NO_NETWORK) {
                            Toast.makeText(this, "Network failed", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(this, "Code error: ${it.errorCode}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAuth()
        configRecyclerView()
        // configFirestore() // carga el listado pero solo una consulta
        // configFirestoreRealtime() // carga el listado en tiempo real
        configButtons()
    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.nsvProducts.visibility = View.VISIBLE
                binding.llProgress.visibility = View.GONE
                binding.efab.show()
            } else {
                // Provider Google Authentication
                // Apertura una UI generica para la bd del proyecto
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false) // Desactivar automcompletado de email
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(
                this@MainActivity,
                3,
                GridLayoutManager.HORIZONTAL,
                false
            )
            adapter = this@MainActivity.adapter
        }

        /*
        (1..20).forEach {
            val product = Product(
                it.toString(),
                "Product $it",
                "Description of product $it",
                "",
                it,
                it * 1.1
            )
            adapter.add(product)
        }
        */

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sign out successful..", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.llProgress.visibility = View.VISIBLE
                            binding.efab.hide()
                        } else {
                            Toast.makeText(this, "No se pudo cerrar la sesión", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configFirestore() {
        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLLECTION)
            .get()
            .addOnSuccessListener { snapshots ->
                for (document in snapshots) {
                    val products = document.toObject(Product::class.java)
                    products.id = document.id
                    adapter.add(products)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error querying data...", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configFirestoreRealtime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLLECTION)

        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, "Error querying data...", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            for (snapshot in snapshots!!.documentChanges) {
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id

                when (snapshot.type) {
                    DocumentChange.Type.ADDED -> adapter.add(product)
                    DocumentChange.Type.MODIFIED -> adapter.update(product)
                    DocumentChange.Type.REMOVED -> adapter.delete(product)
                }
            }
        }
    }

    private fun configButtons() {
        binding.efab.setOnClickListener {
            productSelected = null
            AddDialogFragment().show(
                supportFragmentManager,
                AddDialogFragment::class.java.simpleName
            )
        }
    }

    override fun onClick(product: Product) {
        productSelected = product
        AddDialogFragment().show(
            supportFragmentManager,
            AddDialogFragment::class.java.simpleName
        )

    }

    override fun onLongClick(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLLECTION)
        product.id?.let { id ->
            productRef.document(id)
                .delete()
                .addOnFailureListener {
                    Toast.makeText(this, "Error deleting record...", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun getProductSelected(): Product? = productSelected

}