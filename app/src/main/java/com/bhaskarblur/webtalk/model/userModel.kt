package com.bhaskarblur.webtalk.model

class userModel {

     var username: String = "";
     var email: String = "";
     var password : String = ""
    var status : String = ""

    constructor(username: String, email: String, password: String) {
        this.username = username
        this.email = email
        this.password = password
    }

    constructor(username: String, email: String, password: String, status : String) {
        this.username = username
        this.email = email
        this.password = password
        this.status = status
    }
    constructor() {

    }
}