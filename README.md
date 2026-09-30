# Hotel booking

A Spring Boot API for adding properties, finding available rooms, and managing bookings, payments and cancellations. It uses Java 21, Maven and in-memory storage.

## Build and run

Install JDK 21 or later, then run these commands from the project folder:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

The first command runs the tests and builds the application. The second starts the API at `http://localhost:8080`.

The Maven wrapper is included.
No database or Docker setup is needed.

can also run the JAR produced by the build:

```bash
java -jar target/hotel-booking-0.0.1-SNAPSHOT.jar
```

If Windows, replace `./mvnw` with `.\mvnw.cmd`.

Dates use UTC and prices use INR. These settings are in `src/main/resources/application.properties`.

## Design

Controllers handle request validation and DTO mapping. The business rules live in the services and domain models. Each service interface sits alongside its `*ServiceImpl` class in `service/`. Request and response classes are in `dto/`, and `advice/GlobalExceptionHandler` handles API errors in one place.

The models in `entity/` are immutable Java records. `Booking` checks its own state transitions. An owner can have one property or several, so a standalone hotel and a hotel group use the same model. Storage sits behind repository interfaces, with in-memory implementations for this assignment.

Availability is calculated for each night of the stay, excluding the check-out date. Pending and confirmed bookings count towards occupied rooms. Search and booking use the same calculation. A shared lock covers the availability check and booking creation together, preventing two requests from taking the last room. Search, payment and cancellation use the same lock.

Payment methods implement `PaymentGateway` and are registered in `config/ApplicationConfiguration`. A new method can be added without changing `BookingServiceImpl`. The cancellation rules live behind `CancellationPolicy`, and property filters implement `PropertyFilter`. Adding a filter that needs another request field also requires updating the search DTO and criteria.

The configuration class supplies the clock and currency as well. Services receive a `Clock` so tests can use a fixed date.

## API

| Operation | Endpoint | Success |
|---|---|---|
| Create owner | `POST /owners` | 201 |
| Add property and room types | `POST /owners/{ownerId}/properties` | 201 |
| Search available properties | `GET /properties` | 200 |
| Get property details | `GET /properties/{propertyId}` | 200 |
| Create booking | `POST /bookings` | 201 |
| Get booking | `GET /bookings/{bookingId}` | 200 |
| Pay | `POST /bookings/{bookingId}/payments` | 200 |
| Cancel | `POST /bookings/{bookingId}/cancellations` | 200 |

Search needs `checkIn`, `checkOut` and `guests`. We can also filter by `city`, `locality`, `minPrice`, `maxPrice`, `minStars` and `amenities`. Amenities can be comma-separated or repeated in the query string, and the property must have all of them. City and locality must match the full name, ignoring case. Price limits apply to the nightly room price and include both endpoints. Results only contain room types that fit the guest count, price range and requested dates.

Error responses contain `code`, `message` and `fieldErrors`. Invalid requests return 400, missing resources return 404, and booking or cancellation conflicts return 409. A mock refund failure returns 502. A declined payment returns 200 with a `FAILED` payment attempt; the booking stays `PENDING_PAYMENT`.

## Example flow

With the app running, try these requests in order. Replace each ID placeholder with the value from the previous response. The date commands work on Linux; on other systems, set the two date variables manually using future dates in `YYYY-MM-DD` format.

```bash
BOOKING_CHECK_IN=$(date -u -d '+7 days' +%F)
BOOKING_CHECK_OUT=$(date -u -d '+9 days' +%F)

curl -sS http://localhost:8080/owners \
  -H 'Content-Type: application/json' \
  -d '{"name":"Garden Group","email":"owner@example.com"}'

OWNER_ID='<owner id>'
curl -sS "http://localhost:8080/owners/$OWNER_ID/properties" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Garden Hotel","city":"Bengaluru","locality":"Indiranagar",
    "stars":4,"amenities":["wifi","parking"],
    "roomTypes":[{"name":"Deluxe","guestCapacity":2,"totalRooms":1,"nightlyPrice":2000}]
  }'

PROPERTY_ID='<property id>'
ROOM_TYPE_ID='<roomTypes[0].id>'

curl -sS "http://localhost:8080/properties?city=Bengaluru&checkIn=$BOOKING_CHECK_IN&checkOut=$BOOKING_CHECK_OUT&guests=2&amenities=wifi&minStars=4"

curl -sS http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -d "{\"propertyId\":\"$PROPERTY_ID\",\"roomTypeId\":\"$ROOM_TYPE_ID\",
       \"checkIn\":\"$BOOKING_CHECK_IN\",\"checkOut\":\"$BOOKING_CHECK_OUT\",
       \"guests\":2,\"guestName\":\"Alex\",\"guestEmail\":\"alex@example.com\"}"

BOOKING_ID='<booking id>'
curl -sS "http://localhost:8080/bookings/$BOOKING_ID/payments" \
  -H 'Content-Type: application/json' \
  -d '{"method":"CARD","paymentToken":"ok"}'

curl -sS -X POST "http://localhost:8080/bookings/$BOOKING_ID/cancellations"

curl -sS "http://localhost:8080/properties?city=Bengaluru&checkIn=$BOOKING_CHECK_IN&checkOut=$BOOKING_CHECK_OUT&guests=2"
```

This books one room for two nights, costing INR 4000. The room disappears from search once booked and appears again after cancellation. Repeating a successful payment request before cancellation returns the existing booking without another charge. Repeating cancellation returns the existing cancellation and refund.

Payments are mocked for `CARD`, `UPI` and `WALLET`; method names ignore case. Use `ok` as the payment token for success, `decline` for a failed payment, or `refund-fail` for a successful payment that cannot be refunded. Other nonblank tokens also succeed. Use these test values, not real payment details. Tokens are not saved in booking records.

The `refund-fail` case fails on every refund attempt. A separate test gateway is used in unit tests to check a refund that fails once and succeeds on retry.

## Assumptions

- Each booking reserves one room. The guest count must fit that room type's capacity. Rooms of the same type share an inventory count; individual room numbers are not assigned.
- A booking starts as `PENDING_PAYMENT` and becomes `CONFIRMED` after successful payment. Either state can be cancelled if the policy allows it. A `CANCELLED` booking cannot be paid.
- Pending bookings hold a room, including after a declined payment. Holds do not expire automatically, so unpaid bookings need to be cancelled before check-in to release the room.
- Prices are fixed per room per night, use two decimal places, and share one configured currency. The total is saved when the booking is created. Taxes, fees and dynamic pricing are not included.
- Cancellation is allowed strictly before the check-in date. Paid bookings receive a full refund; unpaid bookings receive zero. If the refund fails, the booking stays confirmed and keeps its room.
- Same-day check-in is allowed, but those bookings are already past the cancellation cutoff. Search, new bookings and new payment attempts reject check-in dates in the past.
- Repeating a successful payment or cancellation does not charge or refund again, even with concurrent requests. This uses the booking state, not an idempotency key. Failed payments can be retried. Repeating a booking creation request can create another booking if a room is available.
- Search shows availability at the time of the request. Booking checks it again before reserving the room.
- All data is lost when the app restarts. Authentication, a frontend and real payment integration are outside this assignment.
- The shared lock works within one running application and is held during mock payment calls. Running multiple instances or using a real gateway would require database transactions, durable payment records and recovery for interrupted payments or refunds.

## Tests

Run the tests with:

```bash
./mvnw test
```

The unit tests cover availability across multiple nights, check-out boundaries, validation, payment and refund failures, and cancellation rules. Concurrency tests check requests competing for the last room, repeated payments and cancellations, and payment happening alongside cancellation.

The REST test starts the app on a random port and runs the full flow: add an owner and property, search, book, pay, cancel, then search again. It also checks error responses. Tests use a fixed clock set to January 1, 2030.

## With more time

The next steps would be to expire unpaid holds, add persistent storage, and make payment retries safe across restarts. Search pagination and OpenAPI documentation would also be useful.
