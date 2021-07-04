# SuperiorSkyblock2 Missions [BrewingMission]
This is an addon JAR for SuperiorSkyblock2.

This will make breeding missions available.

###### [Example mission]
```
missions:
  brewer:
    mission-file: BrewingMission
    island: false
    auto-reward: false
    rewards:
      commands:
        - 'eco give %player% 150'
        - 'is admin msg %player% &e&lMission | &7Successfully finished the mission Birthday!'
    brewings:
      '1':  # SPEED 1 POTION (3:00)
        potionType: SPEED
        extended: false #is the potion extended with redstone dust?
        upgraded: false #is the potion upgraded with glowstone dust?
        splash: false   #is the potion made into splash with gunpowder?
        amount: 5
      '2':  # SPEED 1 POTION (8:00)
        potionType: SPEED
        extended: true #is the potion extended with redstone dust?
        upgraded: false #is the potion upgraded with glowstone dust?
        splash: false   #is the potion made into splash with gunpowder?
        amount: 5
      '3':  # SPEED 2 potion (1:30)
        potionType: SPEED
        extended: false #is the potion extended with redstone dust?
        upgraded: true #is the potion upgraded with glowstone dust?
        splash: false   #is the potion made into splash with gunpowder?
        amount: 5
      '4':  # Any extended potion
        potionType: any-potion-extended
        amount: 5
      '5':  # Any upgraded potion
        potionType: any-potion-upgraded
        amount: 5
      '6':  # Any splash potion
        potionType: any-potion-splash
        amount: 5
    icons:
      not-completed:
        type: POTION
        name: '&aLocal Brewery'
        lore:
          - '&7Brew it!'
          - ''
          - '&6Required Materials:'
          - '&8 - &7{value_SPEED|false|false|false}/5 SPEED 1 POTION (3:00)'
          - '&8 - &7{value_SPEED|true|false|false}/5 SPEED 1 POTION (8:00)'
          - '&8 - &7{value_SPEED|false|true|false}/5 SPEED 2 potion (1:30)'
          - '&8 - &7{value_any-potion-extended}/5 Any extended potion'
          - '&8 - &7{value_any-potion-upgraded}/5 Any upgraded potion'
          - '&8 - &7{value_any-potion-splash}/5 Any splash potion'
          - ''
          - '&6Rewards:'
          - '&8 - &7$150'
          - ''
          - '&6Progress: &7{0}%'
          - '&c&l ✘ &7Not Completed'
      can-complete:
        type: PAPER
        name: '&aLocal Brewery'
        lore:
          - '&7Brew it!'
          - ''
          - '&6Required Materials:'
          - '&8 - &7x5 SPEED 1 POTION (3:00)'
          - '&8 - &7x5 SPEED 1 POTION (8:00)'
          - '&8 - &7x5 SPEED 2 potion (1:30)'
          - '&8 - &7x5 Any extended potion'
          - '&8 - &7x5 Any upgraded potion'
          - '&8 - &7x5 Any splash potion'
          - ''
          - '&6Rewards:'
          - '&8 - &7$150'
          - ''
          - '&6Progress: &7100%'
          - '&a&l ✔ &7Click to redeem your reward.'
        enchants:
          DURABILITY: 1
        flags:
          - HIDE_ENCHANTS
      completed:
        type: MAP
        name: '&aLocal Brewery'
        lore:
          - '&7Brew it!'
          - ''
          - '&6Progress: &7100%'
          - '&a&l ✔ &7Already Claimed.'   
```
