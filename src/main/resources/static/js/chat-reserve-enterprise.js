document.addEventListener('DOMContentLoaded', function () {


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
console.log("reservaId:", reservaId);
if (reservaId) {
    ws.initialize(config.socketUrl, [`/enterprise/reserva/${reservaId}`]);

ws.receive = (msg) => {
      const oldFn = ws.receive; // guarda referencia a manejador anterior
        console.log("Mensaje recibido en el chat de reserva:", msg);
        appendMessage(`${msg.sender}: ${msg.text}`);
    };
}
sendBtn.addEventListener("click", () => {
    const mensaje = chatInput.value.trim();
    if (mensaje !== "") {
        go(`/enterprise/${reservaId}/msg`, "POST", {
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