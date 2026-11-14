package com.example.features.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.core.model.CanvasElement
import com.example.core.model.Page
import com.example.core.model.PageBackground
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class PaperPreset(
  val id: String,
  val nameEn: String,
  val nameBn: String,
  val tintHex: String,
  val pattern: String, // laid | ruled | dot_grid | graph | tea_stained | kraft | airmail | obsidian_gold
  val defaultEdge: String = "clean_cut", // clean_cut | deckle | torn
  val defaultInkHex: String = "#302C29",
  val isDarkPaper: Boolean = false,
)

val PAPER_PRESETS: List<PaperPreset> =
  listOf(
    PaperPreset("warm_ivory", "Warm Ivory", "উষ্ণ আইভরি", "#F6EFE2", "laid", "clean_cut"),
    PaperPreset("antique_parchment", "Antique Parchment", "প্রাচীন পার্চমেন্ট", "#EFE2CA", "tea_stained", "deckle"),
    PaperPreset("cream_cotton", "Cream Cotton", "ক্রিম কটন", "#FAF6EE", "laid", "clean_cut"),
    PaperPreset("ruled", "Classic Ruled", "ক্লাসিক রুলড", "#F7F1E5", "ruled", "clean_cut"),
    PaperPreset("dot_grid", "Dot Grid Journal", "ডট গ্রিড", "#F5EFE3", "dot_grid", "clean_cut"),
    PaperPreset("graph_grid", "Architectural Graph", "গ্রাফ পেপার", "#EEF2EE", "graph", "clean_cut"),
    PaperPreset("tea_stained", "Tea-Stained Letter", "চায়ে ভিজানো চিঠি", "#E6D2B5", "tea_stained", "torn"),
    PaperPreset("kraft_paper", "Pressed Kraft", "ক্রাফট কাগজ", "#D6BA96", "kraft", "torn"),
    PaperPreset("blush_linen", "Morning Blush", "সকালের ব্লাশ", "#F5E4E1", "laid", "clean_cut"),
    PaperPreset("sage_mist", "Botanical Sage", "বোটানিক্যাল সেজ", "#E7EDE2", "ruled", "clean_cut"),
    PaperPreset("lavender_dusk", "Lavender Dusk", "ল্যাভেন্ডার গোধূলি", "#ECE7F2", "dot_grid", "clean_cut"),
    PaperPreset("sky_linen", "Soft Sky Linen", "নীলচে লিনেন", "#E6EEF3", "laid", "clean_cut"),
    PaperPreset("deckle_parchment", "Deckle Archive", "ডেকল আর্কাইভ", "#EDE0C9", "laid", "deckle"),
    PaperPreset("torn_letter", "Torn Manuscript", "ছেঁড়া পাণ্ডুলিপি", "#EFE5D4", "ruled", "torn"),
    PaperPreset("airmail_border", "Vintage Airmail", "ভিন্টেজ এয়ারমেইল", "#F4EFE6", "airmail", "clean_cut"),
    PaperPreset("chamomile_cream", "Chamomile Cream", "ক্যামোমাইল ক্রিম", "#F8F0D8", "dot_grid", "clean_cut"),
    PaperPreset("terracotta_wash", "Terracotta Wash", "পোড়ামাটি ছাপ", "#EDD5CA", "tea_stained", "deckle"),
    PaperPreset("olive_herbarium", "Olive Herbarium", "জলপাই পাতা", "#E3E6D6", "laid", "deckle"),
    PaperPreset("sepia_archive", "Sepia Chronology", "সেপিয়া কালপঞ্জি", "#E2D1B8", "ruled", "torn"),
    PaperPreset("midnight_ink", "Midnight Ink", "রাতের কালি", "#1F2633", "dot_grid", "clean_cut", "#F6EFE2", true),
    PaperPreset("walnut_espresso", "Walnut Espresso", "ওয়ালনাট এস্প্রেসো", "#2B201C", "laid", "deckle", "#F6EFE2", true),
    PaperPreset("obsidian_gold", "Obsidian Gold", "অবসিডিয়ান সোনালি", "#1D1B1A", "obsidian_gold", "clean_cut", "#DFC690", true),
  )

fun findPaperPreset(id: String?): PaperPreset =
  PAPER_PRESETS.firstOrNull { it.id == id } ?: PAPER_PRESETS.first()

enum class StudioCategory {
  ALL,
  PAPERS,
  TEMPLATES,
  WASHI_TAPE,
  FRAMES,
  LABELS,
  STAMPS,
  BOTANICAL_FLORALS,
  EPHEMERA,
  SEASONAL,
  MY_UPLOADS,
  FAVORITES,
}

data class StudioAsset(
  val id: String,
  val category: StudioCategory,
  val elementType: String, // tape | frame | label | sticker
  val nameEn: String,
  val nameBn: String,
  val colorHex: String,
  val secondaryHex: String = "#F6EFE2",
  val defaultW: Float = 0.28f,
  val defaultH: Float = 0.14f,
  val defaultTextEn: String? = null,
  val defaultTextBn: String? = null,
)

val ASSET_PACK_1: List<StudioAsset> =
  listOf(
    // Washi Tapes (Pack 1)
    StudioAsset("tape_gold_foil", StudioCategory.WASHI_TAPE, "tape", "Antique Gold Foil Tape", "সোনালি ফয়েল টেপ", "#B08A4F", "#DFC690", 0.36f, 0.06f),
    StudioAsset("tape_gingham_terra", StudioCategory.WASHI_TAPE, "tape", "Terracotta Gingham Tape", "টেরাকোটা চেক টেপ", "#A83E2B", "#F6EFE2", 0.34f, 0.06f),
    StudioAsset("tape_sage_stripe", StudioCategory.WASHI_TAPE, "tape", "Botanical Sage Stripe", "সেজ ডোরা টেপ", "#5C7052", "#E7EDE2", 0.34f, 0.06f),
    StudioAsset("tape_kraft_grid", StudioCategory.WASHI_TAPE, "tape", "Kraft Grid Masking", "ক্রাফট মাস্কিং টেপ", "#8E6F4E", "#EFE2CA", 0.32f, 0.055f),
    StudioAsset("tape_indigo_stars", StudioCategory.WASHI_TAPE, "tape", "Midnight Star Washi", "নীল তারার টেপ", "#2A4365", "#DFC690", 0.36f, 0.06f),
    StudioAsset("tape_blush_petal", StudioCategory.WASHI_TAPE, "tape", "Rose Petal Washi", "গোলাপি পাপড়ি টেপ", "#C07B7B", "#FAF6EE", 0.32f, 0.055f),

    // Ornamental Frames (Pack 1)
    StudioAsset("frame_double_gold", StudioCategory.FRAMES, "frame", "Gilded Double Frame", "সোনালি ডাবল ফ্রেম", "#B08A4F", "#F6EFE2", 0.62f, 0.36f),
    StudioAsset("frame_vintage_stamp", StudioCategory.FRAMES, "frame", "Postage Mat Frame", "ডাকটিকিট ফ্রেম", "#593B2B", "#FAF6EE", 0.56f, 0.36f),
    StudioAsset("frame_botanical_corner", StudioCategory.FRAMES, "frame", "Leaf Corner Frame", "পাতা কোণা ফ্রেম", "#5C7052", "#F6EFE2", 0.64f, 0.40f),
    StudioAsset("frame_stitched_card", StudioCategory.FRAMES, "frame", "Stitched Memory Card", "সেলাই করা কার্ড ফ্রেম", "#705344", "#EFE2CA", 0.60f, 0.32f),

    // Literary Labels & Ticket Stubs (Pack 1)
    StudioAsset("label_ribbon_gold", StudioCategory.LABELS, "label", "Bookmark Tag", "বুকমার্ক ট্যাগ", "#B08A4F", "#241A16", 0.44f, 0.10f, "Memory Note", "স্মৃতিকথা"),
    StudioAsset("label_kraft_ticket", StudioCategory.LABELS, "label", "Vintage Ticket Stub", "ভিন্টেজ টিকিট", "#C9AC82", "#302C29", 0.46f, 0.11f, "NO. 024 · JOURNAL", "নং ০২৪ · সংকলন"),
    StudioAsset("label_index_tab", StudioCategory.LABELS, "label", "Archival Index Label", "আর্কাইভ লেবেল", "#EFE2CA", "#593B2B", 0.42f, 0.10f, "Chapter / Reflection", "অধ্যায় ও অনুভূতি"),
    StudioAsset("label_crimson_seal_tag", StudioCategory.LABELS, "label", "Crimson Note Strip", "লালচে নোট স্ট্রিপ", "#A83E2B", "#FAF6EE", 0.44f, 0.095f, "Special Day", "বিশেষ দিন"),

    // Vintage Stamps & Wax Seals (Pack 1)
    StudioAsset("stamp_wax_sankalan", StudioCategory.STAMPS, "sticker", "Sankalan Wax Seal (ৎ)", "সংকলন মোমের সীল (ৎ)", "#A83E2B", "#DFC690", 0.18f, 0.14f, "ৎ"),
    StudioAsset("stamp_postage_dhaka", StudioCategory.STAMPS, "sticker", "Air-Mail Postmark", "ডাকঘরের সীল", "#2A4365", "#F6EFE2", 0.24f, 0.16f, "AIR MAIL · 2026"),
    StudioAsset("stamp_coffee_ring", StudioCategory.STAMPS, "sticker", "Morning Coffee Stain", "কফির কাপের দাগ", "#6F4E37", "#EFE2CA", 0.22f, 0.16f),
    StudioAsset("stamp_quill_emblem", StudioCategory.STAMPS, "sticker", "Fountain Quill Crest", "কলম ও নিব সীল", "#593B2B", "#B08A4F", 0.20f, 0.15f),
    StudioAsset("stamp_crescent_moon", StudioCategory.STAMPS, "sticker", "Night Sky Seal", "রাতের চাঁদ তারা", "#2A4365", "#E9C46A", 0.19f, 0.14f),
    StudioAsset("stamp_pressed_leaf", StudioCategory.STAMPS, "sticker", "Herbarium Leaf Stamp", "শুকনো পাতার ছাপ", "#5C7052", "#E7EDE2", 0.20f, 0.16f),
    StudioAsset("stamp_sunburst_badge", StudioCategory.STAMPS, "sticker", "Golden Sunburst", "সোনালি সূর্য", "#B08A4F", "#F8F0D8", 0.19f, 0.14f),
    StudioAsset("stamp_verified_memory", StudioCategory.STAMPS, "sticker", "Cherished Stamp", "অমূল্য স্মৃতি সীল", "#8F392A", "#FAF6EE", 0.23f, 0.14f, "CHERISHED"),
  )

val ASSET_PACK_2_FLORALS: List<StudioAsset> =
  listOf(
    StudioAsset("floral_shapla_lotus", StudioCategory.BOTANICAL_FLORALS, "sticker", "Bengal Water Lily (Shapla)", "বাংলার শাপলা ফুল", "#E6B8BE", "#5C7052", 0.24f, 0.18f),
    StudioAsset("floral_marigold_garland", StudioCategory.BOTANICAL_FLORALS, "sticker", "Marigold Garland", "গাঁদা ফুলের মালা", "#E09F3E", "#5C7052", 0.30f, 0.14f),
    StudioAsset("floral_pressed_fern", StudioCategory.BOTANICAL_FLORALS, "sticker", "Pressed Monsoon Fern", "শুকনো ফার্ন পাতা", "#4E6747", "#E7EDE2", 0.22f, 0.22f),
    StudioAsset("floral_peony_blush", StudioCategory.BOTANICAL_FLORALS, "sticker", "Watercolor Peony", "জলরঙের পিওনি ফুল", "#C96F78", "#FAF6EE", 0.22f, 0.18f),
    StudioAsset("floral_bougainvillea", StudioCategory.BOTANICAL_FLORALS, "sticker", "Bougainvillea Sprig", "বাগানবিলাস ডাল", "#B83B66", "#4F6D48", 0.25f, 0.20f),
    StudioAsset("floral_gold_wreath", StudioCategory.BOTANICAL_FLORALS, "sticker", "Botanical Gold Wreath", "সোনালি পাতার চক্র", "#B08A4F", "#5C7052", 0.24f, 0.20f),
    StudioAsset("tape_floral_meadow", StudioCategory.BOTANICAL_FLORALS, "tape", "Pressed Meadow Washi", "বুনো ফুলের ওয়াশি টেপ", "#6E8360", "#F5E4E1", 0.34f, 0.06f),
    StudioAsset("frame_floral_vine", StudioCategory.BOTANICAL_FLORALS, "frame", "Vine & Blossom Frame", "লতা-পাতার ফ্রেম", "#5C7052", "#DFC690", 0.62f, 0.38f),
  )

val ASSET_PACK_3_EPHEMERA_SEASONAL: List<StudioAsset> =
  listOf(
    // Ephemera
    StudioAsset("ephem_torn_vellum", StudioCategory.EPHEMERA, "label", "Torn Vellum Note", "ছেঁড়া পার্চমেন্ট চিরকুট", "#EFE2CA", "#593B2B", 0.44f, 0.12f, "A quiet note…", "ছোট্ট চিরকুট…"),
    StudioAsset("ephem_film_strip", StudioCategory.EPHEMERA, "frame", "35mm Memory Film Strip", "পুরোনো ফিল্ম স্ট্রিপ", "#241A16", "#DFC690", 0.60f, 0.26f),
    StudioAsset("ephem_archive_receipt", StudioCategory.EPHEMERA, "label", "Bookshop Receipt Slip", "বইয়ের দোকানের রসিদ", "#F4EFE6", "#302C29", 0.44f, 0.14f, "EDITION NO. 07", "সংগ্রহ নং ০৭"),
    StudioAsset("ephem_deckle_tab", StudioCategory.EPHEMERA, "frame", "Stitched Linen Pocket", "সেলাই করা পকেট কার্ড", "#C9AC82", "#302C29", 0.56f, 0.34f),
    // Seasonal & Bangla Heritage
    StudioAsset("season_monsoon_boat", StudioCategory.SEASONAL, "sticker", "Monsoon Paper Boat", "বর্ষার কাগজের নৌকা", "#2A4365", "#EFE2CA", 0.25f, 0.18f),
    StudioAsset("season_autumn_shiuli", StudioCategory.SEASONAL, "sticker", "Autumn Shiuli Blossom", "শরতের শিউলি ফুল", "#FAF6EE", "#D96B27", 0.20f, 0.18f),
    StudioAsset("season_boishakh_alpona", StudioCategory.SEASONAL, "sticker", "Boishakh Alpona Motif", "বৈশাখী আলপনা", "#A83E2B", "#FAF6EE", 0.24f, 0.20f),
    StudioAsset("season_winter_tea", StudioCategory.SEASONAL, "sticker", "Winter Morning Tea", "শীতের সকালের চা", "#8E6F4E", "#FAF6EE", 0.22f, 0.18f),
    StudioAsset("season_spring_krishnochura", StudioCategory.SEASONAL, "sticker", "Krishnochura Glow", "কৃষ্ণচূড়ার রঙ", "#C94A2B", "#5C7052", 0.24f, 0.18f),
    StudioAsset("season_golden_lantern", StudioCategory.SEASONAL, "sticker", "Festival Paper Lantern", "সোনালি ফানুস", "#E09F3E", "#A83E2B", 0.20f, 0.22f),
  )

val ALL_STUDIO_ASSETS: List<StudioAsset> =
  ASSET_PACK_1 + ASSET_PACK_2_FLORALS + ASSET_PACK_3_EPHEMERA_SEASONAL

fun findStudioAsset(assetId: String?): StudioAsset? =
  ALL_STUDIO_ASSETS.firstOrNull { it.id == assetId }

data class PageTemplate(
  val id: String,
  val nameEn: String,
  val nameBn: String,
  val subtitleEn: String,
  val subtitleBn: String,
  val background: PageBackground,
  val suggestedMood: String? = null,
  val suggestedTitleEn: String,
  val suggestedTitleBn: String,
  val elementsFactory: (isBangla: Boolean) -> List<CanvasElement>,
)

val PAGE_TEMPLATES: List<PageTemplate> =
  listOf(
    PageTemplate(
      id = "tpl_morning",
      nameEn = "Morning Reflection",
      nameBn = "সকালের ভাবনা",
      subtitleEn = "Warm ivory with headline ribbon & gratitude box",
      subtitleBn = "শিরোনাম ফিতা ও সকালের ৩টি অনুভূতি",
      background = PageBackground(presetId = "warm_ivory", tintHex = "#F6EFE2", edgeStyle = "clean_cut"),
      suggestedMood = "Peaceful",
      suggestedTitleEn = "Morning Pages",
      suggestedTitleBn = "সকালের পাতা",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "tape", assetId = "tape_gold_foil", x = 0.30f, y = 0.04f, w = 0.40f, h = 0.05f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "label", assetId = "label_ribbon_gold", text = if (bn) "আজকের সকালের ভাবনা" else "Today's Morning Intention", x = 0.10f, y = 0.12f, w = 0.80f, h = 0.08f, colorHex = "#B08A4F", zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "আজকের দিনটি আমি শান্ত মনে শুরু করছি।\n\n১. মনের কথা:\n২. আজ যা করতে চাই:\n৩. ছোট কৃতজ্ঞতা:" else "Starting today with clarity and calm.\n\n1. On my mind:\n2. One gentle goal:\n3. Grateful for:", fontId = if (bn) "noto_serif_bn" else "playfair", fontSize = 15f, colorHex = "#302C29", x = 0.10f, y = 0.24f, w = 0.80f, h = 0.44f, zIndex = 3),
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_coffee_ring", x = 0.68f, y = 0.74f, w = 0.22f, h = 0.16f, opacity = 0.75f, zIndex = 4),
        )
      },
    ),
    PageTemplate(
      id = "tpl_gratitude",
      nameEn = "Gratitude Journal",
      nameBn = "কৃতজ্ঞতার তালিকা",
      subtitleEn = "Dot grid with framed cards & wax seal",
      subtitleBn = "তিনটি ফ্রেম ও মোমের সীলমোহর",
      background = PageBackground(presetId = "dot_grid", tintHex = "#F5EFE3", edgeStyle = "clean_cut"),
      suggestedMood = "Grateful",
      suggestedTitleEn = "Three Gratitudes",
      suggestedTitleBn = "তিনটি কৃতজ্ঞতা",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "frame", assetId = "frame_double_gold", x = 0.08f, y = 0.13f, w = 0.84f, h = 0.23f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "১. প্রিয় মানুষের সাথে সুন্দর মুহূর্ত…" else "1. A quiet kindness I noticed today…", fontId = if (bn) "noto_sans_bn" else "inter", fontSize = 14f, colorHex = "#302C29", x = 0.12f, y = 0.17f, w = 0.76f, h = 0.15f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "frame", assetId = "frame_double_gold", x = 0.08f, y = 0.40f, w = 0.84f, h = 0.23f, zIndex = 3),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "২. নিজের ছোট কোনো অর্জন…" else "2. A small win worth remembering…", fontId = if (bn) "noto_sans_bn" else "inter", fontSize = 14f, colorHex = "#302C29", x = 0.12f, y = 0.44f, w = 0.76f, h = 0.15f, zIndex = 4),
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_wax_sankalan", x = 0.72f, y = 0.72f, w = 0.18f, h = 0.15f, zIndex = 5),
        )
      },
    ),
    PageTemplate(
      id = "tpl_polaroid",
      nameEn = "Polaroid Collage",
      nameBn = "পোলারয়েড স্ক্র্যাপবুক",
      subtitleEn = "Washi tape, photo mat & handwritten caption",
      subtitleBn = "ওয়াশি টেপ ও ছবির ফ্রেমসহ ক্যাপশন",
      background = PageBackground(presetId = "cream_cotton", tintHex = "#FAF6EE", edgeStyle = "clean_cut"),
      suggestedMood = "Joyful",
      suggestedTitleEn = "Captured Moments",
      suggestedTitleBn = "ফ্রেমবন্দী স্মৃতি",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "frame", assetId = "frame_vintage_stamp", x = 0.12f, y = 0.14f, w = 0.76f, h = 0.42f, rotation = -2f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "tape", assetId = "tape_gingham_terra", x = 0.35f, y = 0.11f, w = 0.30f, h = 0.055f, rotation = 4f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "এখানে আপনার প্রিয় ছবি বসান এবং নিচে গল্প লিখুন…" else "Drop a favorite photo inside the frame & write its story below…", fontId = if (bn) "atma" else "caveat", fontSize = 17f, colorHex = "#593B2B", align = "center", x = 0.12f, y = 0.62f, w = 0.76f, h = 0.22f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_travel",
      nameEn = "Travel Postcard",
      nameBn = "ভ্রমণের পোস্টকার্ড",
      subtitleEn = "Airmail parchment with postmark & ticket stub",
      subtitleBn = "এয়ারমেইল বর্ডার, টিকিট ও ডাকটিকিট সীল",
      background = PageBackground(presetId = "airmail_border", tintHex = "#F4EFE6", edgeStyle = "clean_cut"),
      suggestedMood = "Nostalgic",
      suggestedTitleEn = "Postcard From The Road",
      suggestedTitleBn = "পথের ডায়েরি",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_postage_dhaka", x = 0.68f, y = 0.13f, w = 0.24f, h = 0.15f, rotation = 6f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "label", assetId = "label_kraft_ticket", text = if (bn) "গন্তব্য · তারিখ" else "DESTINATION · ROUTE", x = 0.08f, y = 0.15f, w = 0.54f, h = 0.10f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "আজকের যাত্রার সবচেয়ে সুন্দর দৃশ্য ও মানুষের গল্প এখানে লিখে রাখি…" else "Miles traveled, quiet cafés found, and faces remembered along the way…", fontId = if (bn) "noto_serif_bn" else "playfair", fontSize = 15f, colorHex = "#2A4365", x = 0.10f, y = 0.34f, w = 0.80f, h = 0.45f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_reading",
      nameEn = "Reading Log & Quote",
      nameBn = "পড়ার খাতা ও উক্তি",
      subtitleEn = "Book note margin on tea-stained deckle paper",
      subtitleBn = "প্রিয় বইয়ের নাম, উক্তি ও মতামত",
      background = PageBackground(presetId = "antique_parchment", tintHex = "#EFE2CA", edgeStyle = "deckle"),
      suggestedMood = "Focused",
      suggestedTitleEn = "Book Notes",
      suggestedTitleBn = "বইয়ের পাতা থেকে",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "“যা কিছু সুন্দর ও সত্য, তা স্মৃতির পাতায় বেঁচে থাকে।”" else "“We read to know we are not alone.”", fontId = if (bn) "galada" else "playfair", fontSize = 19f, colorHex = "#593B2B", align = "center", italic = true, x = 0.10f, y = 0.15f, w = 0.80f, h = 0.20f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "shape", assetId = "divider", colorHex = "#B08A4F", x = 0.20f, y = 0.38f, w = 0.60f, h = 0.04f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "বইয়ের নাম:\nলেখক:\n\nআমার অনুভূতি ও নোটস:" else "Title:\nAuthor:\n\nKey reflections & margin thoughts:", fontId = if (bn) "noto_sans_bn" else "inter", fontSize = 14f, colorHex = "#302C29", x = 0.10f, y = 0.46f, w = 0.80f, h = 0.40f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_planner",
      nameEn = "Daily Checklist",
      nameBn = "দৈনিক কাজের তালিকা",
      subtitleEn = "Architectural dot grid with archival label",
      subtitleBn = "কাজের তালিকা ও দিনের অগ্রাধিকার",
      background = PageBackground(presetId = "graph_grid", tintHex = "#EEF2EE", edgeStyle = "clean_cut"),
      suggestedMood = "Focused",
      suggestedTitleEn = "Today's Plan",
      suggestedTitleBn = "আজকের পরিকল্পনা",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "label", assetId = "label_index_tab", text = if (bn) "আজকের ৩টি মূল কাজ" else "TOP 3 PRIORITIES", x = 0.08f, y = 0.13f, w = 0.56f, h = 0.09f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "☐  ১.\n☐  ২.\n☐  ৩.\n\nসারাদিনের সংক্ষিপ্ত নোট:" else "☐  Priority one\n☐  Priority two\n☐  Priority three\n\nEvening check-in:", fontId = if (bn) "noto_sans_bn" else "inter", fontSize = 15f, colorHex = "#302C29", x = 0.10f, y = 0.26f, w = 0.80f, h = 0.54f, zIndex = 2),
        )
      },
    ),
    PageTemplate(
      id = "tpl_poetry",
      nameEn = "Poetry & Calligraphy",
      nameBn = "কবিতা ও ক্যালিগ্রাফি",
      subtitleEn = "Deckle parchment paired with Galada & Playfair",
      subtitleBn = "হাতের লেখা ও ক্যালিগ্রাফি ফন্টের পাতা",
      background = PageBackground(presetId = "tea_stained", tintHex = "#E6D2B5", edgeStyle = "torn"),
      suggestedMood = "Reflective",
      suggestedTitleEn = "Quiet Verse",
      suggestedTitleBn = "হৃদয়ের কবিতা",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_quill_emblem", x = 0.40f, y = 0.12f, w = 0.20f, h = 0.14f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "হৃদয়ের গভীর থেকে লেখা\nকয়েকটি পঙ্‌ক্তি এখানে রাখি…" else "Lines written when the evening grows still…", fontId = if (bn) "galada" else "caveat", fontSize = 20f, colorHex = "#302C29", align = "center", x = 0.10f, y = 0.30f, w = 0.80f, h = 0.46f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_wax_sankalan", x = 0.72f, y = 0.78f, w = 0.18f, h = 0.14f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_evening_tea",
      nameEn = "Evening Tea & Mood",
      nameBn = "বিকেলের চা ও ডায়েরি",
      subtitleEn = "Botanical sage ruled sheet with sage tape",
      subtitleBn = "সবুজাভ রুলড কাগজ ও বিকেলের গল্প",
      background = PageBackground(presetId = "sage_mist", tintHex = "#E7EDE2", edgeStyle = "clean_cut"),
      suggestedMood = "Peaceful",
      suggestedTitleEn = "Evening Wind-Down",
      suggestedTitleBn = "সন্ধ্যা বেলার কথা",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "tape", assetId = "tape_sage_stripe", x = 0.08f, y = 0.13f, w = 0.38f, h = 0.055f, rotation = -3f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_pressed_leaf", x = 0.72f, y = 0.13f, w = 0.20f, h = 0.16f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "দিনশেষে মনের ভার কমিয়ে রাখার শান্ত অনুচ্ছেদ…" else "What brought peace today? What can wait for tomorrow?", fontId = if (bn) "noto_serif_bn" else "playfair", fontSize = 15f, colorHex = "#302C29", x = 0.10f, y = 0.30f, w = 0.80f, h = 0.50f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_photo_frame",
      nameEn = "Botanical Photo Card",
      nameBn = "বোটানিক্যাল ফটো কার্ড",
      subtitleEn = "Olive herbarium border for framed memories",
      subtitleBn = "শুকনো পাতার ফ্রেম ও স্মৃতিকথা",
      background = PageBackground(presetId = "olive_herbarium", tintHex = "#E3E6D6", edgeStyle = "deckle"),
      suggestedMood = "Joyful",
      suggestedTitleEn = "Herbarium Frame",
      suggestedTitleBn = "প্রিয় আলোকচিত্র",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "frame", assetId = "frame_botanical_corner", x = 0.10f, y = 0.14f, w = 0.80f, h = 0.46f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "label", assetId = "label_ribbon_gold", text = if (bn) "তারিখ ও স্থান" else "Date & Place", x = 0.22f, y = 0.64f, w = 0.56f, h = 0.09f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "ছবির পেছনের গল্প…" else "Behind this photograph…", fontId = if (bn) "noto_sans_bn" else "inter", fontSize = 14f, align = "center", colorHex = "#302C29", x = 0.12f, y = 0.76f, w = 0.76f, h = 0.16f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_milestone",
      nameEn = "Milestone & Keepsake",
      nameBn = "বিশেষ দিন ও স্মারক",
      subtitleEn = "Obsidian gold edition for unforgettable dates",
      subtitleBn = "কালো-সোনালি আভিজাত্যে বিশেষ দিনের স্মারক",
      background = PageBackground(presetId = "obsidian_gold", tintHex = "#1D1B1A", edgeStyle = "clean_cut"),
      suggestedMood = "Grateful",
      suggestedTitleEn = "Golden Milestone",
      suggestedTitleBn = "সোনালি মাইলফলক",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_sunburst_badge", x = 0.40f, y = 0.12f, w = 0.20f, h = 0.14f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "frame", assetId = "frame_double_gold", x = 0.09f, y = 0.28f, w = 0.82f, h = 0.48f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "একটি অবিস্মরণীয় দিন যা সারাজীবন মনে থাকবে…" else "A milestone worth gilding in gold ink…", fontId = if (bn) "noto_serif_bn" else "playfair", fontSize = 16f, colorHex = "#DFC690", align = "center", x = 0.14f, y = 0.38f, w = 0.72f, h = 0.28f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_botanical",
      nameEn = "Midnight Stargazer",
      nameBn = "রাতের তারাভরা পাতা",
      subtitleEn = "Midnight indigo paper with gold ink & moon seal",
      subtitleBn = "গাঢ় নীল পাতায় সোনালি হরফে রাতের ডায়েরি",
      background = PageBackground(presetId = "midnight_ink", tintHex = "#1F2633", edgeStyle = "clean_cut"),
      suggestedMood = "Reflective",
      suggestedTitleEn = "Midnight Notes",
      suggestedTitleBn = "নিঝুম রাতের পাতা",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "sticker", assetId = "stamp_crescent_moon", x = 0.72f, y = 0.12f, w = 0.19f, h = 0.14f, zIndex = 1),
          CanvasElement(id = UUID.randomUUID().toString(), type = "tape", assetId = "tape_indigo_stars", x = 0.08f, y = 0.13f, w = 0.40f, h = 0.055f, zIndex = 2),
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "রাতের নির্জনতায় নিজের সাথে একটু কথা…" else "Quiet thoughts under the late-night sky…", fontId = if (bn) "atma" else "caveat", fontSize = 18f, colorHex = "#F6EFE2", x = 0.10f, y = 0.28f, w = 0.80f, h = 0.50f, zIndex = 3),
        )
      },
    ),
    PageTemplate(
      id = "tpl_minimal_notes",
      nameEn = "Minimalist Ruled Sheet",
      nameBn = "মিনিমাল রুলড নোট",
      subtitleEn = "Clean lined diary page with archival corner tag",
      subtitleBn = "পরিচ্ছন্ন রুলটানা পাতা ও কর্নার লেবেল",
      background = PageBackground(presetId = "ruled", tintHex = "#F7F1E5", edgeStyle = "clean_cut"),
      suggestedMood = null,
      suggestedTitleEn = "Journal Entry",
      suggestedTitleBn = "দৈনন্দিন লিপি",
      elementsFactory = { bn ->
        listOf(
          CanvasElement(id = UUID.randomUUID().toString(), type = "text", text = if (bn) "আজকের পাতার লেখা এখান থেকে শুরু করুন…" else "Begin writing today's entry here…", fontId = if (bn) "noto_sans_bn" else "inter", fontSize = 15f, colorHex = "#302C29", x = 0.08f, y = 0.15f, w = 0.84f, h = 0.70f, zIndex = 1),
        )
      },
    ),
  )

fun applyTemplateToPage(
  page: Page,
  template: PageTemplate,
  replaceExisting: Boolean,
  isBangla: Boolean,
): Page {
  val generated = template.elementsFactory(isBangla)
  return if (replaceExisting) {
    page.copy(
      title = page.title?.takeIf { it.isNotBlank() } ?: if (isBangla) template.suggestedTitleBn else template.suggestedTitleEn,
      mood = page.mood ?: template.suggestedMood,
      background = template.background,
      elements = generated,
    )
  } else {
    val baseZ = (page.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
    val shifted = generated.mapIndexed { idx, el -> el.copy(zIndex = baseZ + idx) }
    page.copy(
      background = template.background,
      elements = page.elements + shifted,
    )
  }
}

data class UserUploadedAsset(
  val id: String,
  val title: String,
  val filePath: String,
  val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Offline SharedPreferences store for Studio Favorites and My Uploads (downsampled local cutouts).
 */
class StudioUserStore(private val context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val uploadsType = Types.newParameterizedType(List::class.java, UserUploadedAsset::class.java)
  private val uploadsAdapter = moshi.adapter<List<UserUploadedAsset>>(uploadsType)

  fun loadFavorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

  fun toggleFavorite(id: String): Set<String> {
    val next = loadFavorites().toMutableSet()
    if (!next.add(id)) next.remove(id)
    prefs.edit().putStringSet(KEY_FAVORITES, next).apply()
    return next
  }

  fun loadUploads(): List<UserUploadedAsset> {
    val raw = prefs.getString(KEY_UPLOADS, null) ?: return emptyList()
    return runCatching { uploadsAdapter.fromJson(raw) }.getOrNull().orEmpty()
      .filter { File(it.filePath).exists() }
  }

  fun savePickedUserUpload(uri: Uri, title: String? = null): UserUploadedAsset? {
    val path =
      runCatching {
        val dir = File(context.filesDir, "user_assets").apply { mkdirs() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 960 || bounds.outHeight / sample > 960) sample *= 2
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp =
          context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) }
            ?: return null
        val file = File(dir, "asset_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 92, it) }
        bmp.recycle()
        file.absolutePath
      }.getOrNull() ?: return null

    val asset =
      UserUploadedAsset(
        id = "upload_${UUID.randomUUID()}",
        title = title?.trim()?.takeIf { it.isNotEmpty() } ?: "My Sticker",
        filePath = path,
      )
    val current = listOf(asset) + loadUploads()
    prefs.edit().putString(KEY_UPLOADS, uploadsAdapter.toJson(current)).apply()
    return asset
  }

  fun deleteUpload(id: String): List<UserUploadedAsset> {
    val existing = loadUploads()
    existing.firstOrNull { it.id == id }?.let { runCatching { File(it.filePath).delete() } }
    val next = existing.filterNot { it.id == id }
    prefs.edit().putString(KEY_UPLOADS, uploadsAdapter.toJson(next)).apply()
    return next
  }

  companion object {
    private const val PREFS_NAME = "sankalan_studio_store"
    private const val KEY_FAVORITES = "favorite_ids_v1"
    private const val KEY_UPLOADS = "user_uploads_v1"
  }
}
