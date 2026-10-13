package com.kivaisi.selflearn.data

/**
 * Core content model. Organized so that later a chapter's [content] text
 * could be fed to an LLM to auto-generate a [QuestionSet] instead of the
 * hardcoded one below -- the UI and quiz engine only depend on these data
 * classes, not on how they were produced.
 */

enum class QuestionType { SOCRATIC_OPEN, MULTIPLE_CHOICE, TRUE_FALSE }

data class Question(
    val id: String,
    val type: QuestionType,
    val prompt: String,
    // For MULTIPLE_CHOICE: list of options. For TRUE_FALSE: listOf("True", "False").
    val options: List<String> = emptyList(),
    // Index into options that is correct. Unused for SOCRATIC_OPEN.
    val correctIndex: Int = -1,
    // Shown after the answer (right or wrong) tying feedback back to the chapter text.
    val explanation: String,
    // Only used for SOCRATIC_OPEN questions: a model answer shown for self-assessment.
    val modelAnswer: String = "",
    val difficulty: Int = 1 // 1 = easiest, increasing afterwards
)

data class Chapter(
    val id: String,
    val title: String,
    val subject: String,
    val emojiLabel: String, // short subject tag, not used as an icon per design (icons are Compose vector icons)
    val content: String,
    val questions: List<Question>,
    val defaultTimerSeconds: Int = 300
)

object ContentRepository {

    val riversChapter = Chapter(
        id = "rivers_erosion",
        title = "Rivers and Erosion",
        subject = "Geography",
        emojiLabel = "GEO",
        defaultTimerSeconds = 300,
        content = """
Rivers are among the most powerful sculptors of the Earth's surface. As water flows from highland sources toward the sea, it carries enormous energy that constantly reshapes the land through a process called erosion. Erosion is the wearing away and removal of rock and soil by moving water, and it happens in several distinct ways.

The first process is hydraulic action, where the sheer force of moving water loosens particles from the riverbed and banks. Fast-flowing water pushes into cracks and crevices, breaking rock apart purely through pressure. Closely related is abrasion, sometimes called corrasion, in which the river's load of sand, pebbles, and stones acts like sandpaper, scraping and grinding against the channel bed and banks as it is carried downstream. Over time, abrasion can carve deep grooves into solid rock.

A third process, attrition, describes how the rocks and sediment being transported by the river collide with each other. These collisions gradually break larger stones into smaller, smoother, and more rounded fragments, which is why pebbles found far downstream are typically smoother than those near a river's source. Finally, solution (or corrosion) occurs when slightly acidic river water chemically dissolves soluble rock types, such as limestone and chalk, slowly wearing them away without any mechanical scraping at all.

Erosion is not evenly distributed across a river's course. On a meandering river, water flows fastest on the outside of a bend because it has to travel a longer distance in the same time as water on the inside, so it moves with more energy. This faster flow erodes the outer bank more aggressively, often undercutting it and creating steep slopes called river cliffs. Meanwhile, the slower-moving water on the inside of the bend has less energy, so it drops the sediment it is carrying, building up gentle, gradually sloping deposits called slip-off slopes or point bars.

This uneven erosion-and-deposition pattern is what causes meanders to become more pronounced and sinuous over time, sometimes evolving into horseshoe-shaped oxbow lakes when a loop is eventually cut off from the main channel. Near a river's source, where the gradient is steep, vertical erosion (downward cutting) dominates, carving deep V-shaped valleys. Further downstream, as the gradient flattens, lateral erosion (sideways cutting) becomes more important, widening the valley floor into broad floodplains.

Understanding these erosional processes helps explain many everyday landscape features: why riverbanks on bends are undercut and collapse, why gravel near a river's mouth is rounder than rock near its source, and why rivers that once ran straight can end up twisting into dramatic loops across flat land.
        """.trimIndent(),
        questions = listOf(
            Question(
                id = "rivers_q1",
                type = QuestionType.SOCRATIC_OPEN,
                prompt = "Why do you think rivers erode faster on the outside of a bend than on the inside?",
                explanation = "",
                modelAnswer = "On the outside of a bend, water has to travel a longer distance in the same time as the water on the inside, so it moves faster and carries more energy. That extra energy and speed increase hydraulic action and abrasion against the outer bank, eroding it more aggressively and often undercutting it into a steep river cliff. The slower water on the inside has less energy, so instead of eroding, it drops (deposits) sediment, building up a gentle slope.",
                difficulty = 1
            ),
            Question(
                id = "rivers_q2",
                type = QuestionType.TRUE_FALSE,
                prompt = "True or False: Abrasion happens when a river's water chemically dissolves rock like limestone.",
                options = listOf("True", "False"),
                correctIndex = 1,
                explanation = "False -- that process is called solution (or corrosion). Abrasion is when sediment carried by the river scrapes and grinds against the bed and banks, acting like sandpaper.",
                difficulty = 1
            ),
            Question(
                id = "rivers_q3",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which erosion process explains why pebbles get smoother and smaller the further downstream you find them?",
                options = listOf("Hydraulic action", "Attrition", "Solution", "Vertical erosion"),
                correctIndex = 1,
                explanation = "Attrition is correct: rocks and sediment collide with each other as they're carried downstream, breaking them into smaller, smoother, rounder fragments over the journey.",
                difficulty = 2
            ),
            Question(
                id = "rivers_q4",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Near a river's source where the gradient is steep, which type of erosion dominates and what landform does it typically carve?",
                options = listOf(
                    "Lateral erosion, creating wide floodplains",
                    "Vertical erosion, creating deep V-shaped valleys",
                    "Solution, creating oxbow lakes",
                    "Attrition, creating river cliffs"
                ),
                correctIndex = 1,
                explanation = "Vertical (downward) erosion dominates near the steep upper course, cutting deep V-shaped valleys. Lateral erosion becomes more important later, downstream, where the gradient flattens.",
                difficulty = 3
            ),
            Question(
                id = "rivers_q5",
                type = QuestionType.TRUE_FALSE,
                prompt = "True or False: An oxbow lake can form when a meander loop is eventually cut off from the main river channel.",
                options = listOf("True", "False"),
                correctIndex = 0,
                explanation = "True -- as meanders become more sinuous from uneven erosion and deposition, a loop can eventually be cut off entirely from the main flow, leaving a curved oxbow lake behind.",
                difficulty = 3
            )
        )
    )

    val waterCycleChapter = Chapter(
        id = "water_cycle",
        title = "The Water Cycle",
        subject = "Science",
        emojiLabel = "SCI",
        defaultTimerSeconds = 300,
        content = """
Water on Earth is constantly on the move, cycling between the oceans, atmosphere, land, and living things in a continuous process known as the water cycle, or hydrologic cycle. This cycle has no true starting point, but it is often easiest to begin describing it at evaporation.

Evaporation occurs when heat from the sun warms water in oceans, lakes, and rivers, giving water molecules enough energy to escape as invisible water vapor into the atmosphere. Plants contribute to this process too, through transpiration, releasing water vapor from their leaves as part of their normal growth. Together, evaporation and transpiration are often combined into a single term, evapotranspiration, since both move liquid water into the air as vapor.

As water vapor rises higher into the atmosphere, it encounters cooler temperatures. Cooling causes the vapor to lose energy and change back into tiny liquid droplets or ice crystals, a process called condensation. These droplets cluster around dust and other particles in the air, forming the visible clouds we see in the sky. Condensation is essentially the reverse of evaporation: instead of a liquid becoming gas, a gas becomes liquid (or solid, in the case of ice crystals in cold, high clouds).

When enough water droplets gather and combine inside a cloud, they become too heavy for rising air currents to support them, and they fall back to Earth as precipitation. Precipitation can take several forms depending on temperature, including rain, snow, sleet, and hail. This is the main way water returns from the atmosphere to the surface.

Once precipitation reaches the ground, water follows several possible paths. Some of it flows across the land surface into streams, rivers, and eventually oceans or lakes, a process called runoff. Some soaks into the ground through infiltration, replenishing underground water supplies known as groundwater, which can later feed wells, springs, and rivers from below. Some water is taken up by plant roots and eventually transpired back into the air, while some simply evaporates again directly from puddles, soil, or water bodies, restarting the whole cycle.

The water cycle is powered almost entirely by energy from the sun, which drives evaporation, and by gravity, which pulls precipitation and runoff downward. It is also a closed system: the overall amount of water on Earth stays essentially constant, just continuously changing form and location. This endless cycling is what makes freshwater available to ecosystems and human communities across the globe, even though the total water supply never increases or decreases.
        """.trimIndent(),
        questions = listOf(
            Question(
                id = "water_q1",
                type = QuestionType.SOCRATIC_OPEN,
                prompt = "Why do you think water vapor turns back into liquid droplets as it rises higher into the sky?",
                explanation = "",
                modelAnswer = "As water vapor rises, it moves into higher parts of the atmosphere that are cooler. The vapor loses thermal energy to its surroundings, and the molecules slow down and pack closer together, changing from a gas back into tiny liquid droplets (or ice crystals in very cold air) through condensation. This is the direct reverse of evaporation, where heat allowed liquid molecules to gain enough energy to escape as a gas.",
                difficulty = 1
            ),
            Question(
                id = "water_q2",
                type = QuestionType.TRUE_FALSE,
                prompt = "True or False: Transpiration is the release of water vapor from plant leaves.",
                options = listOf("True", "False"),
                correctIndex = 0,
                explanation = "True -- plants release water vapor from their leaves through transpiration, which, combined with evaporation from oceans and lakes, moves water into the atmosphere.",
                difficulty = 1
            ),
            Question(
                id = "water_q3",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which process describes rainwater soaking into the ground to become groundwater?",
                options = listOf("Runoff", "Infiltration", "Condensation", "Transpiration"),
                correctIndex = 1,
                explanation = "Infiltration is correct: water soaks into the soil and rock, replenishing groundwater supplies that can later feed wells, springs, and rivers.",
                difficulty = 2
            ),
            Question(
                id = "water_q4",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "What two main forces power the entire water cycle?",
                options = listOf(
                    "Wind and ocean currents",
                    "The sun's energy and gravity",
                    "Plant respiration and tides",
                    "Volcanic heat and air pressure"
                ),
                correctIndex = 1,
                explanation = "The sun's energy drives evaporation (and evapotranspiration), while gravity pulls precipitation and runoff back down toward the surface -- together these two forces keep the cycle going.",
                difficulty = 3
            ),
            Question(
                id = "water_q5",
                type = QuestionType.TRUE_FALSE,
                prompt = "True or False: The total amount of water on Earth increases slightly every time the water cycle completes a loop.",
                options = listOf("True", "False"),
                correctIndex = 1,
                explanation = "False -- the water cycle is a closed system. The total amount of water on Earth stays essentially constant; it only changes form and location as it moves through evaporation, condensation, precipitation, and runoff/infiltration.",
                difficulty = 3
            )
        )
    )

    val chapters: List<Chapter> = listOf(riversChapter, waterCycleChapter)

    fun findChapter(id: String): Chapter? = chapters.find { it.id == id }
}
