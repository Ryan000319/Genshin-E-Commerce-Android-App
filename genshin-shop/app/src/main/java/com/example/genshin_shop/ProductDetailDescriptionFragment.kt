package com.example.genshin_shop


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProductDetailDescriptionFragment : Fragment() {

    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = arguments?.getParcelable("product") ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.user_fragment_product_description, container, false)
        view.findViewById<TextView>(R.id.productDetailsTextView).text = product.description
        view.findViewById<TextView>(R.id.productStockTextView).text = "Stock: ${product.quantity}"
        return view
    }

    companion object {
        fun newInstance(product: Product): ProductDetailDescriptionFragment {
            val fragment = ProductDetailDescriptionFragment()
            val args = Bundle()
            args.putParcelable("product", product)
            fragment.arguments = args
            return fragment
        }
    }
}
