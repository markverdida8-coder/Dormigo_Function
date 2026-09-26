package com.dormigo;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    // IMPORTANT:
    // This must match your PC's current IPv4 address.
    // Phone and PC must be connected to the same Wi-Fi/network.

    private static final String BASE_URL =
            "http://10.129.224.109/Dormigo_Backend/api/";


    private static final MediaType JSON =
            MediaType.get(
                    "application/json; charset=utf-8"
            );

    private final OkHttpClient client;

    public ApiClient() {
        client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("ngrok-skip-browser-warning", "69420")
                                .build();
                        return chain.proceed(request);
                    }
                })
                .build();
    }

    // =========================================================
    // GET USERS
    // =========================================================

    public void getUsers(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "users.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getUserById(
            int userId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "users.php?user_id="
                        + userId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void updateUser(
            int userId,
            String fullName,
            String email,
            String phone,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "user_id",
                    userId
            );

            if (fullName != null) {
                json.put(
                        "full_name",
                        fullName
                );
            }

            if (email != null) {
                json.put(
                        "email",
                        email
                );
            }

            if (phone != null) {
                json.put(
                        "phone",
                        phone
                );
            }

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "users.php"
                            )
                            .patch(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void deleteUser(int userId, Callback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + "users.php")
                    .delete(body)
                    .build();
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public void login(
            String email,
            String password,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "email",
                    email
            );

            json.put(
                    "password",
                    password
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "login.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // REGISTER USER
    // =========================================================

    public void register(
            String fullName,
            String email,
            String password,
            String phone,
            String userType,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "full_name",
                    fullName
            );

            json.put(
                    "email",
                    email
            );

            json.put(
                    "password",
                    password
            );

            json.put(
                    "phone",
                    phone
            );

            json.put(
                    "user_type",
                    userType
            );

            json.put(
                    "profile_image",
                    JSONObject.NULL
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "register.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET BOARDING HOUSES
    // =========================================================

    public void getBoardingHouses(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "boarding_houses.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // GET ROOMS
    // =========================================================

    public void getRooms(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "rooms.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getRoomsForHouse(
            int houseId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "rooms.php?house_id="
                        + houseId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // CREATE ROOM
    // =========================================================

    public void createRoom(
            int houseId,
            String roomNumber,
            String roomType,
            int capacity,
            double monthlyRent,
            String status,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "house_id",
                    houseId
            );

            json.put(
                    "room_number",
                    roomNumber
            );

            json.put(
                    "room_type",
                    roomType
            );

            json.put(
                    "capacity",
                    capacity
            );

            json.put(
                    "monthly_rent",
                    monthlyRent
            );

            json.put(
                    "status",
                    status
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "rooms.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // UPDATE ROOM
    // =========================================================

    public void updateRoom(
            int roomId,
            String roomNumber,
            String roomType,
            int capacity,
            double monthlyRent,
            String status,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "room_id",
                    roomId
            );

            json.put(
                    "room_number",
                    roomNumber
            );

            json.put(
                    "room_type",
                    roomType
            );

            json.put(
                    "capacity",
                    capacity
            );

            json.put(
                    "monthly_rent",
                    monthlyRent
            );

            json.put(
                    "status",
                    status
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "rooms.php"
                            )
                            .patch(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // UPDATE ROOM STATUS
    // =========================================================

    public void updateRoomStatus(
            int roomId,
            String status,
            Callback callback
    ) {
        try {
            JSONObject json = new JSONObject();
            json.put("room_id", roomId);
            json.put("status", status);

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            MediaType.parse(
                                    "application/json; charset=utf-8"
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "rooms.php"
                            )
                            .patch(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // DELETE ROOM
    // =========================================================

    public void deleteRoom(
            int roomId,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "room_id",
                    roomId
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "rooms.php"
                            )
                            .delete(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    public void createBooking(
            int userId,
            int roomId,
            String moveInDate,
            int durationMonths,
            String status,
            double agreedMonthlyRent,
            double agreedTotalAmount,
            String messageToLandlord,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "user_id",
                    userId
            );

            json.put(
                    "room_id",
                    roomId
            );

            json.put(
                    "move_in_date",
                    moveInDate
            );

            json.put(
                    "duration_months",
                    durationMonths
            );

            json.put(
                    "status",
                    status
            );

            json.put(
                    "agreed_monthly_rent",
                    agreedMonthlyRent
            );

            json.put(
                    "agreed_total_amount",
                    agreedTotalAmount
            );

            json.put(
                    "message_to_landlord",
                    messageToLandlord == null
                            ? ""
                            : messageToLandlord.trim()
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "bookings.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET BOOKINGS
    // =========================================================

    public void getBookings(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "bookings.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getBookingsForHouse(
            int houseId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "bookings.php?house_id="
                        + houseId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getBookingsForLandlord(
            int landlordId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "bookings.php?landlord_id="
                        + landlordId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // GET BOOKING BY ID
    // =========================================================

    public void getBookingById(
            int bookingId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "bookings.php?booking_id="
                        + bookingId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // UPDATE BOOKING STATUS
    // LANDLORD: APPROVE / DECLINE
    // =========================================================

    public void updateBookingStatus(
            int bookingId,
            String status,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "booking_id",
                    bookingId
            );

            json.put(
                    "status",
                    status
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "bookings.php"
                            )
                            .patch(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET ALL PAYMENTS
    // =========================================================

    public void getPayments(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "payments.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // GET PAYMENTS FOR BOOKING
    // =========================================================

    public void getPaymentsForBooking(
            int bookingId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "payments.php?booking_id="
                        + bookingId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getPaymentsForUser(
            int userId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "payments.php?user_id="
                        + userId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getPaymentsForLandlord(
            int landlordId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "payments.php?landlord_id="
                        + landlordId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // GET PAYMENT BY ID
    // =========================================================

    public void getPaymentById(
            int paymentId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "payments.php?payment_id="
                        + paymentId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // CREATE PAYMENT
    // =========================================================

    public void createPayment(
            int bookingId,
            int paymentPeriod,
            String dueDate,
            double amount,
            String paymentMethod,
            String paymentDate,
            String status,
            String transactionRef,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "booking_id",
                    bookingId
            );

            json.put(
                    "payment_period",
                    paymentPeriod
            );

            json.put(
                    "due_date",
                    dueDate
            );

            json.put(
                    "amount",
                    amount
            );

            json.put(
                    "payment_method",
                    paymentMethod
            );

            if (
                    paymentDate == null
                            || paymentDate.trim().isEmpty()
            ) {

                json.put(
                        "payment_date",
                        JSONObject.NULL
                );

            } else {

                json.put(
                        "payment_date",
                        paymentDate.trim()
                );
            }

            json.put(
                    "status",
                    status
            );

            if (
                    transactionRef == null
                            || transactionRef.trim().isEmpty()
            ) {

                json.put(
                        "transaction_ref",
                        JSONObject.NULL
                );

            } else {

                json.put(
                        "transaction_ref",
                        transactionRef.trim()
                );
            }

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "payments.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // UPDATE PAYMENT STATUS
    // =========================================================

    public void updatePaymentStatus(
            int paymentId,
            String status,
            String paymentDate,
            String transactionRef,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "payment_id",
                    paymentId
            );

            json.put(
                    "status",
                    status
            );

            if (
                    paymentDate == null
                            || paymentDate.trim().isEmpty()
            ) {

                json.put(
                        "payment_date",
                        JSONObject.NULL
                );

            } else {

                json.put(
                        "payment_date",
                        paymentDate.trim()
                );
            }

            if (
                    transactionRef == null
                            || transactionRef.trim().isEmpty()
            ) {

                json.put(
                        "transaction_ref",
                        JSONObject.NULL
                );

            } else {

                json.put(
                        "transaction_ref",
                        transactionRef.trim()
                );
            }

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "payments.php"
                            )
                            .patch(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET ALL REVIEWS
    // =========================================================

    public void getReviews(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "reviews.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // GET REVIEWS FOR BOARDING HOUSE
    // =========================================================

    public void getReviewsForHouse(
            int houseId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "reviews.php?house_id="
                        + houseId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    public void getEligibleReviews(int userId, Callback callback) {
        String url = BASE_URL + "get_eligible_reviews.php?user_id=" + userId;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(callback);
    }

    public void getReviewsForUser(int userId, Callback callback) {
        String url = BASE_URL + "reviews.php?user_id=" + userId;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(callback);
    }

    // =========================================================
    // CREATE REVIEW
    // =========================================================

    public void createReview(
            int userId,
            int houseId,
            int rating,
            String comment,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "user_id",
                    userId
            );

            json.put(
                    "house_id",
                    houseId
            );

            json.put(
                    "rating",
                    rating
            );

            if (
                    comment == null
                            || comment.trim().isEmpty()
            ) {

                json.put(
                        "comment",
                        JSONObject.NULL
                );

            } else {

                json.put(
                        "comment",
                        comment.trim()
                );
            }

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "reviews.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET ALL AMENITIES
    // =========================================================

    public void getAmenities(
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "amenities.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // GET BOARDING HOUSE AMENITIES
    // =========================================================

    public void getBoardingHouseAmenities(
            int houseId,
            Callback callback
    ) {

        String url =
                BASE_URL
                        + "boarding_house_amenities.php?house_id="
                        + houseId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(callback);
    }

    // =========================================================
    // ADD AMENITY TO BOARDING HOUSE
    // =========================================================

    public void addBoardingHouseAmenity(
            int houseId,
            int amenityId,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "house_id",
                    houseId
            );

            json.put(
                    "amenity_id",
                    amenityId
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "boarding_house_amenities.php"
                            )
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // REMOVE AMENITY FROM BOARDING HOUSE
    // =========================================================

    public void removeBoardingHouseAmenity(
            int houseId,
            int amenityId,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "house_id",
                    houseId
            );

            json.put(
                    "amenity_id",
                    amenityId
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "boarding_house_amenities.php"
                            )
                            .delete(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    // =========================================================
// UPDATE BOARDING HOUSE
// =========================================================

    public void updateBoardingHouse(
            int houseId,
            int landlordId,
            String houseName,
            String description,
            String address,
            String houseRules,
            String status,
            Callback callback
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "house_id",
                    houseId
            );

            json.put(
                    "landlord_id",
                    landlordId
            );

            json.put(
                    "house_name",
                    houseName
            );

            json.put(
                    "description",
                    description
            );

            json.put(
                    "address",
                    address
            );

            json.put(
                    "house_rules",
                    houseRules
            );

            json.put(
                    "status",
                    status
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "boarding_houses.php"
                            )
                            .patch(body)
                            .build();

            client.newCall(request)
                    .enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // Save the basic house record. Other listing sections require separate endpoints.
    public void createBoardingHouse(
            int landlordId,
            String houseName,
            String description,
            String address,
            String houseRules,
            Callback callback
    ) {
        JSONObject json = new JSONObject();
        try {
            json.put("landlord_id", landlordId);
            json.put("house_name", houseName);
            json.put("description", description);
            json.put("address", address);
            json.put("house_rules", houseRules);
            json.put("status", "INACTIVE");
        } catch (JSONException e) {
            Request request = new Request.Builder()
                    .url(BASE_URL + "boarding_houses.php").build();
            callback.onFailure(client.newCall(request),
                    new IOException("Unable to prepare house details.", e));
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "boarding_houses.php")
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void createBoardingHouseWithRoom(
            int landlordId, String houseName, String description,
            String address, String houseRules, String roomNumber,
            String roomType, int capacity, String monthlyRent,
            Callback callback
    ) {
        createBoardingHouseWithRoom(landlordId, houseName, description,
                address, houseRules, roomNumber, roomType, capacity, monthlyRent,
                new int[0], callback);
    }

    public void createBoardingHouseWithRoom(
            int landlordId, String houseName, String description,
            String address, String houseRules, String roomNumber,
            String roomType, int capacity, String monthlyRent,
            int[] amenityIds, Callback callback
    ) {
        JSONObject json = new JSONObject();
        try {
            json.put("landlord_id", landlordId);
            json.put("house_name", houseName);
            json.put("description", description);
            json.put("address", address);
            json.put("house_rules", houseRules);
            json.put("status", "INACTIVE");
            JSONObject room = new JSONObject();
            room.put("room_number", roomNumber);
            room.put("room_type", roomType);
            room.put("capacity", capacity);
            // Send money as decimal text to preserve the entered amount.
            room.put("monthly_rent", monthlyRent);
            room.put("status", "INACTIVE");
            json.put("room", room);
            JSONArray amenities = new JSONArray();
            if (amenityIds != null) {
                for (int id : amenityIds) amenities.put(id);
            }
            json.put("amenity_ids", amenities);
        } catch (JSONException e) {
            Request request = new Request.Builder()
                    .url(BASE_URL + "boarding_houses.php").build();
            callback.onFailure(client.newCall(request),
                    new IOException("Unable to prepare house and room details.", e));
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "boarding_houses.php")
                .post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public void uploadBoardingHouse(
            Context context, JSONObject payload,
            List<Uri> photoUris, Callback callback
    ) {
        final Context appContext = context.getApplicationContext();
        final String payloadText = payload.toString();
        final List<Uri> uris = new ArrayList<>(photoUris);
        final String url = BASE_URL + "boarding_houses.php";
        new Thread(() -> {
            List<File> stagedFiles = new ArrayList<>();
            try {
                MultipartBody.Builder multipart = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("payload", payloadText);
                if (uris.size() < 1) {
                    throw new IOException("Choose at least 1 photo.");
                }
                for (int i = 0; i < uris.size(); i++) {
                    File file = File.createTempFile("house_upload_", ".tmp", appContext.getCacheDir());
                    stagedFiles.add(file);
                    try (InputStream input = appContext.getContentResolver().openInputStream(uris.get(i));
                         OutputStream output = new FileOutputStream(file)) {
                        if (input == null) throw new IOException("Cannot open photo. Select it again.");
                        byte[] buffer = new byte[8192];
                        long total = 0;
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            total += count;
                            if (total > 5L * 1024 * 1024) throw new IOException("Each photo must be 5 MB or smaller.");
                            output.write(buffer, 0, count);
                        }
                        if (total == 0) throw new IOException("A selected photo is empty.");
                    }
                    multipart.addFormDataPart("photos[]", "photo_" + i + ".bin",
                            RequestBody.create(file, MediaType.get("application/octet-stream")));
                }
                Request request = new Request.Builder().url(url).post(multipart.build()).build();
                OkHttpClient uploadClient = client.newBuilder()
                        .retryOnConnectionFailure(false)
                        .writeTimeout(90, TimeUnit.SECONDS)
                        .readTimeout(90, TimeUnit.SECONDS)
                        .build();
                uploadClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        deleteUploadFiles(stagedFiles);
                        callback.onFailure(call, new IOException(
                                "Could not confirm the save. Check your house list before retrying to avoid a duplicate.", e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            callback.onResponse(call, response);
                        } finally {
                            deleteUploadFiles(stagedFiles);
                        }
                    }
                });
            } catch (Exception e) {
                deleteUploadFiles(stagedFiles);
                Request request = new Request.Builder().url(url).build();
                callback.onFailure(client.newCall(request), new IOException(
                        "Upload was not started. " + (e.getMessage() == null ? "Select the photos again." : e.getMessage()), e));
            }
        }, "house-photo-upload").start();
    }

    private void deleteUploadFiles(List<File> files) {
        for (File file : files) {
            if (file.exists()) file.delete();
        }
    }

    // =========================================================
    // MESSAGES / CHAT API
    // =========================================================

    public void getChatConversations(int userId, Callback callback) {
        String url = BASE_URL + "messages.php?user_id=" + userId;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(callback);
    }

    public void getChatThread(int userId, int otherUserId, Callback callback) {
        String url = BASE_URL + "messages.php?user_id=" + userId + "&other_user_id=" + otherUserId;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(callback);
    }

    public void sendMessage(int senderId, int receiverId, Integer houseId, String messageText, Callback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("sender_id", senderId);
            json.put("receiver_id", receiverId);
            if (houseId != null) {
                json.put("house_id", houseId);
            }
            json.put("message_text", messageText);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + "messages.php")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteBoardingHouse(int houseId, Callback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("house_id", houseId);
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + "boarding_houses.php")
                    .delete(body)
                    .build();
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
