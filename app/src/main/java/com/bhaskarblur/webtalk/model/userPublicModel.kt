package com.bhaskarblur.webtalk.model

class userPublicModel {

     var username: String = "";
     var email: String = "";
    var status : String = ""

    constructor(username: String, email: String,password: String) {
        this.username = username
        this.email = email
    }

    constructor(username: String, email: String, password: String, status : String) {
        this.username = username
        this.email = email
        this.status = status
    }
    constructor() {

    }
}