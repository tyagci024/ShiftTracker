package com.example.shifttracker.view

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.shifttracker.R
import com.example.shifttracker.databinding.FragmentLoginScreenBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class LoginScreen : Fragment() {
    private lateinit var binding: FragmentLoginScreenBinding
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //FirebaseApp.initializeApp(requireContext())
        auth = FirebaseAuth.getInstance()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_login_screen, container, false)
        binding.logObject=this

        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToProfile()
        }




        return binding.root
    }

    fun loginButton (){
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Kullanıcı başarıyla giriş yaptı


                    Toast.makeText(requireContext(), "Giriş işlemi başarılı", Toast.LENGTH_LONG).show()
                    // TimelineFragment'a git
                    navigateToProfile()
                } else {
                    // Giriş işlemi başarısız oldu, hata mesajını görüntüle
                    val exception = task.exception
                    Toast.makeText(requireContext(), "Giriş işlemi başarısız: ${exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun registerButton(){

        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Kullanıcı başarıyla kaydedildi
                    Toast.makeText(requireContext(), "Kayıt işlemi başarılı", Toast.LENGTH_LONG).show()
                    // TimelineFragment'a git

                    navigateToProfile()
                } else {
                    // Kayıt işlemi başarısız oldu, hata mesajını görüntüle
                    val exception = task.exception
                    Toast.makeText(requireContext(), "Kayıt işlemi başarısız: ${exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun navigateToProfile (){
        findNavController().navigate(R.id.action_loginScreen_to_profilePage)

    }


}