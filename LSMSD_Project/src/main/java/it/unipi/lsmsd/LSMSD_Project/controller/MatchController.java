package it.unipi.lsmsd.LSMSD_Project.controller;

import it.unipi.lsmsd.LSMSD_Project.model.GameStatistic;
import it.unipi.lsmsd.LSMSD_Project.model.Match;
import it.unipi.lsmsd.LSMSD_Project.model.User;
import it.unipi.lsmsd.LSMSD_Project.service.MatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @PostMapping("/add")
    public ResponseEntity<?> addMatch(@RequestBody Match match, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            Match newMatch = matchService.addMatch(match);
            return ResponseEntity.ok(newMatch);
        } else {
            return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllMatches(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null && currentUser.isAdmin()) {
            List<Match> matches = matchService.getAllMatches();
            if (!matches.isEmpty()) {
                return ResponseEntity.ok(matches);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/getByUser")
    public ResponseEntity<?> getMatchesByUser(@RequestParam(required = false) String user, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        if (user == null) {
            List<Match> matches = matchService.getMatchesByUser(currentUser.getUsername());
            if (!matches.isEmpty()) {
                return ResponseEntity.ok(matches);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            if (currentUser.isAdmin()) {
                List<Match> matches = matchService.getMatchesByUser(user);
                if (!matches.isEmpty()) {
                    return ResponseEntity.ok(matches);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
            }
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getGameStatistics(
            @RequestParam(required = false) Integer minMatches,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean ascending,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        //if (true){
        if(currentUser != null && currentUser.isAdmin()) {
            List<GameStatistic> statistics = matchService.getGameStatistics(minMatches, limit, ascending);
            if (!statistics.isEmpty()) {
                return ResponseEntity.ok(statistics);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
        }
    }
}
