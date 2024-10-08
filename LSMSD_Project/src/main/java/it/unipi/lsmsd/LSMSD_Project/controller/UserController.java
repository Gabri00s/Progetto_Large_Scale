package it.unipi.lsmsd.LSMSD_Project.controller;

import it.unipi.lsmsd.LSMSD_Project.model.BoardGame;
import it.unipi.lsmsd.LSMSD_Project.model.Game;
import it.unipi.lsmsd.LSMSD_Project.model.User;
import it.unipi.lsmsd.LSMSD_Project.projections.UserOnlyUsernameProjection;
import it.unipi.lsmsd.LSMSD_Project.projections.UserProfileProjection;
import it.unipi.lsmsd.LSMSD_Project.projections.UserUsernameProjection;
import it.unipi.lsmsd.LSMSD_Project.service.BoardGameService;
import it.unipi.lsmsd.LSMSD_Project.service.UserService;
import it.unipi.lsmsd.LSMSD_Project.utils.UserAlreadyExistsException;
import it.unipi.lsmsd.LSMSD_Project.utils.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;


import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BoardGameService boardGameService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            return new ResponseEntity<>("User is already logged in and cannot register again.", HttpStatus.FORBIDDEN);
        }

        try {
            User registeredUser = userService.registerNewUser(user);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (UserAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            return new ResponseEntity<>("User is already logged in.", HttpStatus.FORBIDDEN);
        }

        try {
            User user = userService.authenticate(username, password);
            session.setAttribute("user", user);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (InvalidCredentialsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }




    @GetMapping("/getAll")
    public ResponseEntity<List<UserUsernameProjection>> getAllUsers(@RequestParam(required = false) String name, @RequestParam int n) {
        List<UserUsernameProjection> users;
        if (name != null && !name.isEmpty()) {
            users = userService.findUsernamesByPartialName(name);
        } else {
            users = userService.getAllUsernames(n);
        }
        return new ResponseEntity<>(users, HttpStatus.OK);
    }


    @GetMapping("/getOnlyUsername")
    public ResponseEntity<UserOnlyUsernameProjection> getOnlyUsername(@RequestParam String username) {
        UserOnlyUsernameProjection user = userService.getOnlyUsernameByUsername(username);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            User user = userService.getUserByUsername(currentUser.getUsername());
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpSession session) {
        session.invalidate();
        return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
    }
    @DeleteMapping("/deleteAccount")
    public ResponseEntity<String> deleteAccount(@RequestParam(required = false) String username, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        if (username == null) {
            userService.deleteUserByUsername(currentUser.getUsername());
            session.invalidate();
            return ResponseEntity.ok("Account eliminato con successo.");
        } else {
            if (currentUser.isAdmin()) {
                User userToDelete = userService.getUserByUsername(username);
                if (userToDelete == null) {
                    return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
                }

                userService.deleteUserByUsername(username);
                return ResponseEntity.ok("Account eliminato con successo.");
            } else {
                return new ResponseEntity<>("Operazione non autorizzata", HttpStatus.UNAUTHORIZED);
            }
        }
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentialsException(InvalidCredentialsException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam String username, HttpSession session) {
            UserProfileProjection user = userService.getUserProfile(username);
            if (user != null) {
                return new ResponseEntity<>(user, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }
    }

    @PutMapping("/profile/update")
    public ResponseEntity<?> updateUserProfile(HttpSession session, @RequestBody User updatedUser) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null && currentUser.getUsername() == updatedUser.getUsername()) {
            User user = userService.updateUserProfile(updatedUser, currentUser.getUsername());
            if (user != null) {
                session.setAttribute("user", user);
                return new ResponseEntity<>(user, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>("User not authenticated or not have the permission", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/library")
    public ResponseEntity<?> getUserLibrary(@RequestParam String username) {
        List<Game> library = userService.getUserLibrary(username);
        if (library != null) {
            return new ResponseEntity<>(library, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found or library is empty", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/addLibrary")
    public ResponseEntity<?> addGameToLibrary(@RequestParam Long gameId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            BoardGame boardGame = boardGameService.getBoardGameByGameId(gameId);
            if (boardGame == null) {
                return new ResponseEntity<>("Game not found", HttpStatus.NOT_FOUND);
            }

            Game game = new Game(boardGame.getGameId(), boardGame.getName());

            if(userService.addGameToLibrary(currentUser.getUsername(), game)){
                return new ResponseEntity<>("Game added to library", HttpStatus.OK);
            }

            return new ResponseEntity<>("Game already in library", HttpStatus.CONFLICT);
        } else {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping("/removeLibrary")
    public ResponseEntity<?> removeGameFromLibrary(@RequestParam Long gameId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            BoardGame boardGame = boardGameService.getBoardGameByGameId(gameId);
            if (boardGame == null) {
                return new ResponseEntity<>("Game not found", HttpStatus.NOT_FOUND);
            }

            Game game = new Game(boardGame.getGameId(), boardGame.getName());

            if(userService.removeGameFromLibrary(currentUser.getUsername(), game)){
                return new ResponseEntity<>("Game removed from library", HttpStatus.OK);
            }

            return new ResponseEntity<>("Game not in library", HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
    }



}

