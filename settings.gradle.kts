rootProject.name = "FawayMC"

// ===== Lobby =====
include(
    ":Lobby:Advancement",
    ":Lobby:Daily",
    ":Lobby:Friend",
    ":Lobby:Global",
    ":Lobby:Leaderboard",
    ":Lobby:Level",
    ":Lobby:Luckyblock"
)
project(":Lobby:Advancement").projectDir    = file("Lobby/Advancement")
project(":Lobby:Daily").projectDir          = file("Lobby/Daily")
project(":Lobby:Friend").projectDir         = file("Lobby/Friend")
project(":Lobby:Global").projectDir         = file("Lobby/Global")
project(":Lobby:Leaderboard").projectDir    = file("Lobby/Leaderboard")
project(":Lobby:Level").projectDir          = file("Lobby/Level")
project(":Lobby:Luckyblock").projectDir     = file("Lobby/Luckyblock")

// ===== Bedwars =====
include(":Bedwars:Global")
project(":Bedwars:Global").projectDir = file("Bedwars/Global")

// ===== Bungee =====
include(":Bungee")
project(":Bungee").projectDir = file("Bungee")