import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams } from 'ionic-angular';



@IonicPage()
@Component({
  selector: 'page-bookings',
  templateUrl: 'bookings.html',
})
export class BookingsPage {
    bookings: string;

    constructor(public navCtrl: NavController, public navParams: NavParams) {
        this.bookings = "currentBookings";
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad BookingsPage');
  }

}
