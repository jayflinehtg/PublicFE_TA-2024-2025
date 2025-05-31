package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.EventSink

@Composable
fun WalletComponent(
    isConnecting: Boolean,
//    balance: String?,
    isGuest: Boolean,
    eventSink: (EventSink) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE6F1E9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Akses Tanaman Herbal",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Hubungkan dompet Anda untuk berkontribusi & menjelajahi dunia tanaman herbal!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // Periksa apakah pengguna adalah tamu
                    if (isGuest) {
                        // Jika tamu, tampilkan informasi tamu
                        Text(
                            text = "Anda sedang menggunakan aplikasi sebagai Tamu.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Tombol Connect atau GuestLogin berdasarkan status isGuest
                    if (isGuest) {
                        // Tombol untuk melanjutkan sebagai tamu
                        TextButton(onClick = { eventSink(EventSink.GuestLogin) }) {
                            Text(
                                text = "Lanjutkan sebagai Tamu (Tanpa Wallet)",
                                fontSize = 10.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    } else {
                        // Tombol untuk menghubungkan wallet
                        Button(
                            onClick = { eventSink(EventSink.Connect) },
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(color = Color.White)
                            } else {
                                Text("Connect Wallet", fontSize = 14.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("or", fontSize = 12.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tautan untuk melanjutkan sebagai tamu
                        TextButton(onClick = { eventSink(EventSink.GuestLogin) }) {
                            Text(
                                text = "Lanjutkan sebagai Tamu (Tanpa Wallet)",
                                fontSize = 10.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
            }
        }
    }
}