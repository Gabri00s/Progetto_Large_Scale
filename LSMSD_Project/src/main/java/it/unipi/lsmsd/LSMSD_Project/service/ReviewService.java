package it.unipi.lsmsd.LSMSD_Project.service;

import it.unipi.lsmsd.LSMSD_Project.dao.BoardGameRepository;
import it.unipi.lsmsd.LSMSD_Project.dao.ReviewRepository;
import it.unipi.lsmsd.LSMSD_Project.model.BoardGame;
import it.unipi.lsmsd.LSMSD_Project.model.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BoardGameRepository boardGameRepository;

    public Review addReview(Review review) {
        String username = review.getUsername();
        Long gameId = review.getGameId();

        List<Review> reviews = reviewRepository.findByUsernameAndGameId(username, gameId);

        if(!reviews.isEmpty()){
            return null;
        }

        return reviewRepository.save(review);
    }

    public boolean deleteReview(String username, Long gameId) {
        System.out.println(username);
        System.out.println(gameId);
        List<Review> reviews = reviewRepository.findByUsernameAndGameId(username, gameId);
        System.out.println(reviews);
        if (!reviews.isEmpty()) {
            reviewRepository.deleteAll(reviews);
            return true;
        }
        return false;
    }

    public List<Review> getReviewsByUsername(String username) {
        return reviewRepository.findByUsername(username);
    }

    public List<Review> getReviewsByGameId(Long gameId) {
        return reviewRepository.findByGameId(gameId);
    }

    public double getAverageRatingByGameId(Long gameId) {
        List<Review> reviews = reviewRepository.findByGameId(gameId);

        return reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);
    }


    public List<Review> findReviewByUserAndGameId(String username, Long gameId) {
        return reviewRepository.findByUsernameAndGameId(username, gameId);
    }

    public List<Review> getRecentReviews(Long gameId, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("gameId").is(gameId));
        query.with(Sort.by(Sort.Direction.DESC, "date"));
        query.limit(limit);
        query.fields().include("username").include("rating").include("review text");

        return mongoTemplate.find(query, Review.class);
    }

    public List<Review> getFilteredReviews(Long gameId, Integer minRating, Integer maxRating, Integer limit) {
        Query query = new Query();

        query.addCriteria(Criteria.where("gameId").is(gameId));


        if (minRating != null && maxRating != null) {
            query.addCriteria(Criteria.where("rating").gte(minRating).lte(maxRating));
        } else if (minRating != null) {
            query.addCriteria(Criteria.where("rating").gte(minRating));
        } else if (maxRating != null) {
            query.addCriteria(Criteria.where("rating").lte(maxRating));
        }

        query.with(Sort.by(Sort.Direction.DESC, "date"));

        if (limit != null && limit > 0) {
            query.limit(limit);
        }

        return mongoTemplate.find(query, Review.class);
    }

    public List<Review> getTopNReviews(Long gameId, int n) {
        Query query = new Query();

        query.addCriteria(Criteria.where("gameId").is(gameId));

        query.with(Sort.by(Sort.Direction.DESC, "rating"));

        query.limit(n);

        return mongoTemplate.find(query, Review.class);
    }

    public List<Review> getLowestNReviews(Long gameId, int n) {
        Query query = new Query();

        query.addCriteria(Criteria.where("gameId").is(gameId));

        query.with(Sort.by(Sort.Direction.ASC, "rating"));

        query.limit(n);

        return mongoTemplate.find(query, Review.class);
    }

    public void updateAllBoardGameRatings(String date) {
        List<Review> recentReviews;
        if (date != null) {
            recentReviews = reviewRepository.findReviewsAfterDate(date);
        } else {
            recentReviews = reviewRepository.findAll();
        }

        Set<Long> gameIds = recentReviews.stream().map(Review::getGameId).collect(Collectors.toSet());
        List<BoardGame> boardGames = boardGameRepository.findByGameIdIn(gameIds);

        for (BoardGame game : boardGames) {
            float averageRating = (float) getAverageRatingByGameId(game.getGameId());
            game.setRating(averageRating);
            boardGameRepository.save(game);
        }
    }


    public void updateAllBoardGameReviews(String date) {
        List<Review> recentReviews;
        if (date != null) {
            recentReviews = reviewRepository.findReviewsAfterDate(date);
        } else {
            recentReviews = reviewRepository.findAll();
        }

        Set<Long> gameIds = recentReviews.stream().map(Review::getGameId).collect(Collectors.toSet());
        List<BoardGame> boardGames = boardGameRepository.findByGameIdIn(gameIds);

        for (BoardGame game : boardGames) {
            List<Review> latestReviews = getRecentReviews(game.getGameId(), 5);
            List<Review> filteredReviews = latestReviews.stream()
                    .map(review -> {
                        Review filteredReview = new Review();
                        filteredReview.setUsername(review.getUsername());
                        filteredReview.setRating(review.getRating());
                        filteredReview.setReviewText(review.getReviewText());
                        return filteredReview;
                    })
                    .collect(Collectors.toList());


            game.setReviews(filteredReviews);
            boardGameRepository.save(game);
        }
    }

}
