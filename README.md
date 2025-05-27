# Mire

It's a nonviolent MUD. (Multi-User Dungeon)

## About this branch
This branch is intended to store features that are currently being under development.

## Project Structure

*   **`project.clj`**: Defines the project, its dependencies (Clojure, server-socket), and the main entry point (`mire.server`).
*   **`src/mire/`**: Contains the core game logic.
    *   **`server.clj`**: Handles server setup, client connections, and the main game loop. This is the entry point of the application.
    *   **`player.clj`**: Defines player-related data and functions (e.g., player state, inventory).
    *   **`rooms.clj`**: Manages room definitions, navigation, and interactions within rooms.
    *   **`commands.clj`**: Parses and executes player commands.
*   **`resources/rooms/`**: Contains definitions for different rooms in the game (e.g., `closet`, `hallway`).
*   **`test/`**: Contains test code for the project, with tests for commands (`test_commands.clj`) and rooms (`test_rooms.clj`).
*   **`lein`**: A Leiningen script for managing the Clojure project (e.g., running, testing, building).

## Architecture

The project follows a typical Clojure project structure. The core game logic is separated into namespaces within the `src/mire` directory. The main entry point is `mire.server`, which likely initializes the server and game state. Game data, such as room descriptions, is stored in files within the `resources` directory.

## Current Functionality

*   **Server:** Handles multiple client connections, allowing for a multiplayer experience.
*   **Game World:** The world is composed of interconnected rooms. Each room has:
    *   A textual description.
    *   Exits to other rooms (e.g., north, south, east, west).
    *   Items that can be picked up.
    *   A list of players currently in the room.
*   **Player Actions (Commands):
    *   `look`: Provides a description of the current room, including exits, items, and other players.
    *   `move <direction>` (or `north`, `south`, `east`, `west`): Allows the player to move between connected rooms.
    *   `grab <item>`: Lets the player pick up an item from the current room and add it to their inventory.
    *   `discard <item>`: Allows the player to drop an item from their inventory into the current room.
    *   `inventory`: Displays the contents of the player's inventory.
    *   `detect <item>`: If the player possesses a "detector" item, this command can locate which room a specific item is in.
    *   `say <message>`: Enables the player to send a message that will be seen by all other players in the same room.
    *   `help`: Displays a list of all available commands and a brief description of what they do.
*   **Items:** Players can interact with items by picking them up and dropping them. Some items, like the "detector", may provide special abilities.
*   **Inventory:** Players maintain an inventory of items they are carrying.
*   **Multiplayer Interaction:** Players can see other players in the same room and communicate with them using the `say` command.
*   **State Management:** The game uses Clojure's STM (Software Transactional Memory) with `ref`s and `dosync` to manage game state concurrently and safely for multiple players.

## New Features Added

### Special Login Greetings
*   **Personalized Welcome Messages:** The game now recognizes specific names during login and displays special greetings:
    *   **Alexander Prutsky** (variations: "Alex P", "Alex Prtutsky", "Alexander Prutsky") - displays "Welcome sensei, hope you appreciate the work of your student!"
    *   **Damir Koblev** (variations: "Damir K", "Damir Koblev") - displays "Welcome, master!"
    *   Name matching is case-insensitive

### Enhanced Commands
*   **`coke`**: A refreshing command that displays "Aaaah, now we're talking" - perfect for those coding moments!
*   **`map`**: Displays a visual ASCII representation of the game world showing:
    *   All rooms and their connections
    *   Current player location
    *   Connection types (| for north/south, -- for east/west)

### Room 102 - Clojure Examination Room
*   **Interactive Exam System:** Room 102 features a comprehensive Clojure programming exam:
    *   **Entry Message:** Upon entering, displays Alexander Prutsky's introduction and "Hello! I am Alexander Prutsky, the master of the functional and recursive programming."
    *   **Three Questions:** Tests basic Clojure knowledge:
        1. What is the result of `(+ 1 2 3)`? (Answer: 6)
        2. What function is used to add an element to the beginning of a list? (Answer: cons)
        3. What does `(first [1 2 3])` return? (Answer: 1)
    *   **Natural Input:** Players can answer directly without typing "answer" - just type the response
    *   **Exam States:** 
        *   Failed exam allows retaking by leaving and re-entering the room
        *   Passed exam shows congratulations on future visits
    *   **Smart Command Handling:** During exam, unrecognized input is treated as answers, while normal commands (move, look, etc.) still work

### Game World Expansion
*   **New Room Layout:** The game world now includes room 102 accessible from the hallway:
    ```
          [closet]      
              |         
          [start]       
              |         
         [hallway] -- [102]
              |         
        [promenade]     
    ```


