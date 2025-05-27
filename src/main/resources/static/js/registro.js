document.addEventListener("DOMContentLoaded", function(){

    console.log("Entramos en el Script")

    document.getElementById("role").addEventListener("change", function(){

        var role = document.getElementById("role").value; //Pillamos si vale ENTERPRISE o USER.
        console.log(role);

        if (role == "ENTERPRISE") {
            document.getElementById("enterpriseAttributes").style.display ="block";
            document.getElementById("parkerAttributes").style.display ="none"; //No mostramos nada.
            document.getElementById("titulo-formulario").innerHTML = "Registro de Empresa";
        }
        else if (role == "USER") {
            document.getElementById("enterpriseAttributes").style.display ="none";
            document.getElementById("parkerAttributes").style.display ="block"; //No mostramos nada.
            document.getElementById("titulo-formulario").innerHTML = "Registro de Parker";
        }
    });
})