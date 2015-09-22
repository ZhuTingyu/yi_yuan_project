function debug(something) {
    if (window.console) {
        if (something instanceof Date) {
            something = something.toDateString();
        }

        if (typeof something == 'object') {
            something = JSON.stringify(something);
        }

        console.log(something);
    }
}

function isEmpty(str) {
    if (str == undefined || str == null || str == "null" || str == "") {
        return true;
    }

    return false;
}
