var qUrl;
var cnt=0;


/** 
 * Upcall to Java sending the time and the name of the accesed page for the final page 
 */

function quit() {
    
    java.exit();
}

/** 
 * Upcall to Java sending the time and the page accessed in a dom element
 */
function sendElementTrace() {
    // var id = $('#accordion .in').parent().attr("id");
    var id = event.srcElement.parentNode.parentNode.parentNode.id;
    java.elementTrace(id);

}

function sendElementTraceQ() {
    // var id = $('#accordion .in').parent().attr("id");
    var id = event.srcElement.id;
    java.elementTrace(id);

}



function checkAnswer() {
    var message = 'Try again',
            selected = document.querySelector('input[value="correct"]:checked'),
            messageDiv = document.querySelector('#message');
    messageDiv.style.color = "red";

    var radios = document.getElementsByClassName("question_item");
    var ans;
    for (var i = 0, length = radios.length; i < length; i++) {
        if (radios[i].checked) {
            ans = radios[i].value;
        }
    }


    if (selected) {


        message = 'Correct';
        messageDiv.style.color = "green";
        java.URLToNextQuestion(qUrl);
        setNextUrl();
    }
    var pageName = document.title;
    var s = "_Answer: " + ans;
    java.elementTrace(s);
    messageDiv.innerHTML = message;
}

function sendUrl() {
    var url = window.location.pathname;
    java.getUrl(url);
}


function setNextUrl() {

    var a = document.getElementById('quiz_next'); //or grab it by tagname etc
    a.href = nextUrl;


}


function backToQuiz() {

    var q = document.getElementById('back'); //or grab it by tagname etc
    q.href = qUrl;
    sendElementTrace();

}


function checkFinalAnswers() {
    var radios = document.getElementsByClassName("question_item");
    var ans;
    for (var i = 0, length = radios.length; i < length; i++) {
        if (radios[i].checked) {
            ans = radios[i].name+"_Answer: " + radios[i].value;
            java.elementTrace(ans);

        }
    }
}