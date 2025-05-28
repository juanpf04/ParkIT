document.addEventListener("DOMContentLoaded", () => {

const abrirChatBtn = document.getElementById("abrirChatBtn");
const cerrarChatBtn = document.getElementById("cerrarChatBtn");
const chatContainer = document.getElementById("chatContainer");
const chatInput = document.getElementById("chatInput");
const chatBox = document.getElementById("chatMessages");
const sendBtn = document.getElementById("sendChatBtn");

function appendMessage(text) {
    const div = document.createElement("div");
    div.className = "chat-message";
    div.textContent = text;
    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}
// Mostrar/ocultar chat
abrirChatBtn.addEventListener("click", () => {
    chatContainer.style.display = "block";
    abrirChatBtn.style.display = "none";
});
cerrarChatBtn.addEventListener("click", () => {
    chatContainer.style.display = "none";
    abrirChatBtn.style.display = "block";
});

const reservaId = document.getElementById("reservaId").value;
if(reservaId){
    ws.subscribe(`/user/reserva/${reservaId}`);
    ws.receive = (msg) => {
    appendMessage(`${msg.from}: ${msg.text}`);
    console.log("Mensaje recibido en el chat de reserva:", msg);
    };
}
sendBtn.addEventListener("click", () => {
    const mensaje = chatInput.value.trim();
    if (mensaje !== "") {
        go(`/user/${reservaId}/msg`, "POST", {
            message: mensaje
        })
        .then(d => {
            console.log("happy", d);
            appendMessage(`Yo: ${mensaje}`);
            chatInput.value = ""; // Limpiar input
        })
        .catch(e => console.log("sad", e));
    }
});

});
