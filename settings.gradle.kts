rootProject.name = "FawayMC"

// All
include(":Advancement",
    ":Daily", ":Friend", ":Global",
    ":Leaderboard", ":Level", ":Luckyblock",
    "Bungee")

// Path to lobby
project(":Advancement").projectDir = file("Lobby/Advancement")
project(":Daily").projectDir = file("Lobby/Daily")
project(":Friend").projectDir = file("Lobby/Friend")
project(":Global").projectDir = file("Lobby/Global")
project(":Leaderboard").projectDir = file("Lobby/Leaderboard")
project(":Level").projectDir = file("Lobby/Level")
project(":Luckyblock").projectDir = file("Lobby/Luckyblock")