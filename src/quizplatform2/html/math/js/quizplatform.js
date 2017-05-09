var qUrl;
var docs;

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

function sendElementTraceQF() {
    // var id = $('#accordion .in').parent().attr("id");
    var id = event.srcElement.name+"_"+event.srcElement.id;
    java.elementTrace(id);

}

function iframeElementTrace(trace) {
    java.elementTrace(trace);
}

function iframeElementTraceCorrect(trace) {
    java.elementTrace(trace);
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
    var s = "Answer: " + ans;
    java.elementTrace(s);
    messageDiv.innerHTML = message;
}



function setNextUrl() {

    var a = document.getElementById('quiz_next'); //or grab it by tagname etc
    a.href = nextUrl;


}


function backToQuiz() {

    var q = document.getElementById('back'); //or grab it by tagname etc
    q.href = qUrl;
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


function qTrace() {

    title = "" + document.getElementById("mbd").contentDocument.title;
    java.elementTrace(title);
}

function setDocuments() {
    var divDoc = document.getElementById("documents");
    for (var i = 0; i < docs.length; i++)
        divDoc.innerHTML += "<ul><a href=\'" + docs[i][0] + "\'>" + docs[i][1] + "</a></ul>";
}


function print() {

    java.print(docs.length);
}


function collectInfo() {

    var message = document.getElementById('message');

    var info = document.getElementsByClassName('form-control');
    var infoRadio = document.getElementsByClassName('form-control-radio');
    var ans;
    for (var i = 0, length = infoRadio.length; i < length; i++) {
        if (infoRadio[i].checked) {
            ans = infoRadio[i].name + ": " + infoRadio[i].value;
            java.elementTrace(ans);

        }
    }
    for (var i = 0, length = info.length; i < length; i++) {

        if (!info[i].value ) {
            java.print(i);
            message.style.display = "";
            
            return;
        }
    }
    for (var i = 0, length = info.length; i < length; i++) {

        ans = info[i].id + ": " + info[i].value;
        java.elementTrace(ans);

    }
    quit();
}
