package it.unipi.lsmsd.LSMSD_Project.controller;

import it.unipi.lsmsd.LSMSD_Project.model.Review;
import it.unipi.lsmsd.LSMSD_Project.model.User;
import it.unipi.lsmsd.LSMSD_Project.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/add")
    public ResponseEntity<?> addReview(@RequestBody Review review, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            Review newReview = reviewService.addReview(review);
            if (newReview != null) {
                return ResponseEntity.ok(newReview);
            } else {
                return ResponseEntity.status(409).body("Hai già inserito una review per questo gioco");
            }
        }else {
            return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteReview(@RequestParam(required = false) String username, @RequestParam Long gameId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        if (username == null) {
            username=currentUser.getUsername();
        }else{
            if (!currentUser.isAdmin()){
                return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
            }
        }
        boolean deleted = reviewService.deleteReview(username, gameId);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recensione non trovata");
        }
    }

    @GetMapping("/getByUsername")
    public ResponseEntity<?> getReviewsByUsername(@RequestParam(required = false) String username,HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        if (username == null) {
            username=currentUser.getUsername();
        }else{
            if (!currentUser.isAdmin()){
                return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
            }
        }
        List<Review> reviews = reviewService.getReviewsByUsername(username);
        if (reviews.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/getByGame")
    public ResponseEntity<List<Review>> getReviewsByGame(@RequestParam Long gameId) {
        List<Review> reviews = reviewService.getReviewsByGameId(gameId);
        if (reviews.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/averageRating")
    public ResponseEntity<?> getAverageRatingByGame(@RequestParam Long gameId) {
        double averageRating = reviewService.getAverageRatingByGameId(gameId);
        return ResponseEntity.ok(averageRating);
    }

    @GetMapping("/findReview")
    public ResponseEntity<Review> getReviewByUserAndGame(@RequestParam String username, @RequestParam Long gameId) {
        List<Review> reviews = reviewService.findReviewByUserAndGameId(username, gameId);

        if (reviews.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else if (reviews.size() > 1) {
            return ResponseEntity.status(500).body(null);
        } else {
            return ResponseEntity.ok(reviews.get(0));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Review>> getRecentReviews(@RequestParam Long gameId, int num) {
        try {
            List<Review> recentReviews = reviewService.getRecentReviews(gameId, num);
            return new ResponseEntity<>(recentReviews, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/filtered")
    public ResponseEntity<List<Review>> getFilteredReviews(
            @RequestParam Long gameId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Integer limit) {

        List<Review> reviews = reviewService.getFilteredReviews(gameId, minRating, maxRating, limit);

        if (reviews.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/top-or-lowest")
    public ResponseEntity<List<Review>> getTopOrLowestNReviews(
            @RequestParam Long gameId,
            @RequestParam int n,
            @RequestParam(defaultValue = "true") boolean highest) {

        List<Review> reviews;

        if (highest) {
            reviews = reviewService.getTopNReviews(gameId, n);
        } else {
            reviews = reviewService.getLowestNReviews(gameId, n);
        }

        if (reviews.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviews);
    }
}
