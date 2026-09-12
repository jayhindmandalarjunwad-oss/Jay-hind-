package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.*
import java.util.Calendar

/**
 * सण, जयंती व राष्ट्रीय विशेष दिनांचा डेटा मॉडेल
 */
data class FestivalItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val greetingWish: String,
    val emojiBadge: String,
    val gradientColors: List<Color>,
    val drawableRes: Int,
    val isNational: Boolean = false
)

object FestivalRegistry {

    /**
     * युजरने दिलेल्या सर्व ७०+ सण, जयंत्या व विशेष दिनांचा समृद्ध अधिकृत कॅटलॉग
     */
    val ALL_FESTIVALS = listOf(
        // १. जानेवारी
        FestivalItem(
            id = "savitribai_phule_jayanti",
            title = "क्रांतीज्योती सावित्रीबाई फुले जयंती",
            subtitle = "॥ स्त्री शिक्षणाच्या जननी • ज्ञानज्योती ॥",
            greetingWish = "क्रांतीज्योती सावित्रीबाई फुले यांच्या जयंतीनिमित्त व बालिका दिनानिमित्त विनम्र अभिवादन!",
            emojiBadge = "📖",
            gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "swami_vivekanand_jayanti",
            title = "स्वामी विवेकानंद जयंती • राष्ट्रीय युवा दिन",
            subtitle = "॥ उत्तिष्ठत जाग्रत प्राप्य वरान्निबोधत ॥",
            greetingWish = "युवकांचे प्रेरणास्थान स्वामी विवेकानंद यांच्या जयंतीनिमित्त व राष्ट्रीय युवा दिनाच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronPrimary, Color(0xFFD84315), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "makar_sankrant",
            title = "मकर संक्रांत",
            subtitle = "॥ तीळगुळ घ्या, गोड गोड बोला ॥",
            greetingWish = "मकर संक्रांतीच्या गोड सणाच्या सर्व बंधू-भगिनींना हार्दिक शुभेच्छा!",
            emojiBadge = "🪁",
            gradientColors = listOf(SaffronPrimary, Color(0xFF0D9488), GoldenTertiary),
            drawableRes = R.drawable.ic_gudi_padwa
        ),
        FestivalItem(
            id = "netaji_subhash_bose_jayanti",
            title = "नेताजी सुभाषचंद्र बोस जयंती",
            subtitle = "॥ तुम मुझे खून दो, मैं तुम्हें आजादी दूंगा ॥",
            greetingWish = "महान स्वातंत्र्यसेनानी नेताजी सुभाषचंद्र बोस यांच्या जयंतीनिमित्त पराक्रम दिनी विनम्र अभिवादन!",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(Color(0xFFE65100), NavySecondary, Color(0xFF1B5E20)),
            drawableRes = R.drawable.ic_tiranga_independence,
            isNational = true
        ),
        FestivalItem(
            id = "voters_day",
            title = "राष्ट्रीय मतदार दिन",
            subtitle = "॥ मतदानाचा हक्क • लोकशाहीचा कणा ॥",
            greetingWish = "राष्ट्रीय मतदार दिनाच्या सर्व सुजाण नागरिकांना हार्दिक शुभेच्छा! मतदान हा आपला पवित्र अधिकार आहे.",
            emojiBadge = "🗳️",
            gradientColors = listOf(NavySecondary, Color(0xFF1565C0), Color(0xFF0D47A1)),
            drawableRes = R.drawable.ic_fest_national_emblem,
            isNational = true
        ),
        FestivalItem(
            id = "republic_day",
            title = "प्रजासत्ताक दिन",
            subtitle = "॥ भारतीय प्रजासत्ताक दिन चिरायू होवो ॥",
            greetingWish = "समस्त ग्रामस्थ व देशवासियांना भारतीय प्रजासत्ताक दिनाच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(Color(0xFFFF9933), Color(0xFF1E3A8A), Color(0xFF138808)),
            drawableRes = R.drawable.ic_tiranga_independence,
            isNational = true
        ),
        FestivalItem(
            id = "martyrs_day",
            title = "हुतात्मा दिन",
            subtitle = "॥ देशासाठी बलिदान देणाऱ्या अमर हुतात्म्यांना नमन ॥",
            greetingWish = "मातृभूमीच्या रक्षणासाठी प्राणांची आहुती देणाऱ्या वीर हुतात्म्यांना व राष्ट्रपिता महात्मा गांधींना विनम्र अभिवादन!",
            emojiBadge = "🕊️",
            gradientColors = listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF455A64)),
            drawableRes = R.drawable.ic_fest_gandhi,
            isNational = true
        ),

        // २. फेब्रुवारी
        FestivalItem(
            id = "vasant_panchami",
            title = "वसंत पंचमी • सरस्वती पूजन",
            subtitle = "॥ विद्या, कला आणि संगीत साधना ॥",
            greetingWish = "वसंत पंचमी व ज्ञानदेवता सरस्वती पूजनाच्या सर्व विद्यार्थ्यांना व सभासदांना हार्दिक शुभेच्छा!",
            emojiBadge = "🪕",
            gradientColors = listOf(Color(0xFFFBC02D), Color(0xFFFFA000), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "shivaji_maharaj_jayanti",
            title = "छत्रपती शिवाजी महाराज जयंती",
            subtitle = "॥ जय भवानी, जय शिवाजी ॥",
            greetingWish = "अखंड महाराष्ट्राचे कुलदैवत छत्रपती शिवाजी महाराज यांच्या जयंतीनिमित्त त्रिवार मानाचा मुजरा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronDark, Color(0xFFB91C1C), GoldenTertiary),
            drawableRes = R.drawable.ic_shivaji_maharaj
        ),
        FestivalItem(
            id = "science_day",
            title = "राष्ट्रीय विज्ञान दिन",
            subtitle = "॥ वैज्ञानिक दृष्टिकोन • प्रगतीची वाटचाल ॥",
            greetingWish = "राष्ट्रीय विज्ञान दिनाच्या सर्व विद्यार्थी व नागरिकांना शुभेच्छा! वैज्ञानिक विचारांचा अंगीकार करूया.",
            emojiBadge = "🔬",
            gradientColors = listOf(Color(0xFF00695C), Color(0xFF00897B), Color(0xFF4DB6AC)),
            drawableRes = R.drawable.ic_fest_knowledge,
            isNational = true
        ),
        FestivalItem(
            id = "mahashivratri",
            title = "महाशिवरात्री",
            subtitle = "॥ हर हर महादेव • ॐ नमः शिवाय ॥",
            greetingWish = "देवाधिदेव महादेव शंकराच्या पावन पर्वावर सर्वांना सुख, समाधान व आरोग्य लाभो हीच प्रार्थना!",
            emojiBadge = "🔱",
            gradientColors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_shiva
        ),

        // ३. मार्च
        FestivalItem(
            id = "holi",
            title = "होळी पौर्णिमा / हुताशनी महोत्सव",
            subtitle = "॥ वाईटावर चांगल्याचा विजय ॥",
            greetingWish = "होळीच्या पवित्र अग्नीत सर्व दुःखे भस्मसात होवोत व जीवनात आनंदाचे रंग भरोत! होळीच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🔥",
            gradientColors = listOf(Color(0xFFD84315), Color(0xFFE64A19), Color(0xFFFF8F00)),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "dhulivandan",
            title = "धुळवड व रंगपंचमी",
            subtitle = "॥ रंगांची उधळण, स्नेहाचे बंध ॥",
            greetingWish = "धुळवड व रंगपंचमीच्या सर्व सभासदांना व ग्रामस्थांना सप्तरंगी मनःपूर्वक शुभेच्छा!",
            emojiBadge = "🎨",
            gradientColors = listOf(Color(0xFFC2185B), Color(0xFF7B1FA2), Color(0xFF303F9F)),
            drawableRes = R.drawable.ic_fest_krishna
        ),
        FestivalItem(
            id = "sant_tukaram_beej",
            title = "संत तुकाराम महाराज बीज",
            subtitle = "॥ आम्हा घरी धन शब्दांचीच रत्ने ॥",
            greetingWish = "जगद्गुरु संतश्रेष्ठ तुकाराम महाराज यांच्या वैकुंठगमन बीजनिमित्त त्यांच्या चरणी कोटी कोटी नमन!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronPrimary, Color(0xFFE65100), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_vithal
        ),
        FestivalItem(
            id = "gudi_padwa",
            title = "गुढीपाडवा • हिंदू नववर्ष",
            subtitle = "॥ नववर्षाची नवी पहाट, सुख समृद्धीची नवी लाट ॥",
            greetingWish = "मराठी नूतन वर्षाच्या व गुढीपाडव्याच्या सर्व अर्जुनवाड ग्रामस्थांना मंगलमय शुभेच्छा!",
            emojiBadge = "🌿",
            gradientColors = listOf(SaffronPrimary, GoldenTertiary, SaffronDark),
            drawableRes = R.drawable.ic_gudi_padwa
        ),
        FestivalItem(
            id = "shri_ram_navami",
            title = "श्रीराम नवमी",
            subtitle = "॥ मर्यादा पुरुषोत्तम प्रभू श्रीरामचंद्र की जय ॥",
            greetingWish = "प्रभू श्रीरामाच्या पावन जन्मोत्सवाच्या सर्व रामभक्तांना मनःपूर्वक भक्तीमय शुभेच्छा!",
            emojiBadge = "🏹",
            gradientColors = listOf(SaffronDark, Color(0xFFD97706), GoldenTertiary),
            drawableRes = R.drawable.ic_shri_ram_festival
        ),

        // ४. एप्रिल
        FestivalItem(
            id = "hanuman_jayanti",
            title = "श्री हनुमान जन्मोत्सव",
            subtitle = "॥ पवनपुत्र हनुमान की जय • जय श्रीराम ॥",
            greetingWish = "शक्ती, भक्ती व बुद्धीचे प्रतीक मारुतीरायांच्या पावन जन्मोत्सवाच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(Color(0xFFE65100), Color(0xFFBF360C), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_hanuman
        ),
        FestivalItem(
            id = "mahatma_phule_jayanti",
            title = "महात्मा ज्योतिराव फुले जयंती",
            subtitle = "॥ सत्यशोधक समाज संस्थापक • क्रांतीसूर्य ॥",
            greetingWish = "सामाजिक क्रांतीचे प्रणेते क्रांतीसूर्य महात्मा ज्योतिराव फुले यांच्या जयंतीनिमित्त विनम्र अभिवादन!",
            emojiBadge = "🌹",
            gradientColors = listOf(SaffronPrimary, Color(0xFFC2185B), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "dr_ambedkar_jayanti",
            title = "भारतरत्न डॉ. बाबासाहेब आंबेडकर जयंती",
            subtitle = "॥ भारतीय संविधानाचे शिल्पकार • ज्ञानसूर्य ॥",
            greetingWish = "विश्वभूषण, भारतरत्न डॉ. बाबासाहेब आंबेडकर यांच्या जयंतीनिमित्त कोटी कोटी प्रणाम!",
            emojiBadge = "💙",
            gradientColors = listOf(NavySecondary, Color(0xFF1E40AF), Color(0xFF2563EB)),
            drawableRes = R.drawable.ic_fest_dharmachakra
        ),
        FestivalItem(
            id = "mahavir_jayanti",
            title = "भगवान महावीर जयंती",
            subtitle = "॥ अहिंसा परमो धर्मः ॥",
            greetingWish = "जैन धर्मियांचे २४ वे तीर्थंकर भगवान महावीर यांच्या पावन जयंतीनिमित्त मनःपूर्वक सदिच्छा!",
            emojiBadge = "🕊️",
            gradientColors = listOf(Color(0xFFF57F17), Color(0xFFFBC02D), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_dharmachakra
        ),
        FestivalItem(
            id = "akshaya_tritiya",
            title = "अक्षय तृतीया",
            subtitle = "॥ साडेतीन मुहूर्तांपैकी एक शुभ मुहूर्त ॥",
            greetingWish = "अक्षय तृतीयेच्या व परशुराम जयंतीच्या सर्व ग्रामस्थांना मंगलमय शुभेच्छा! जीवनात अक्षय सुख लाभो.",
            emojiBadge = "🪙",
            gradientColors = listOf(Color(0xFFFFB300), Color(0xFFFFA000), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),

        // ५. मे
        FestivalItem(
            id = "maharashtra_din",
            title = "महाराष्ट्र दिन व कामगार दिन",
            subtitle = "॥ जय जय महाराष्ट्र माझा, गर्जा महाराष्ट्र माझा ॥",
            greetingWish = "महाराष्ट्र दिन व आंतरराष्ट्रीय कामगार दिनाच्या सर्व कष्टकरी बंधू-भगिनींना हार्दिक शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronDark, NavySecondary, GoldenTertiary),
            drawableRes = R.drawable.ic_shivaji_maharaj,
            isNational = true
        ),
        FestivalItem(
            id = "buddha_pournima",
            title = "बुद्ध पौर्णिमा",
            subtitle = "॥ बुद्धं शरणं गच्छामि • शांती व करुणा ॥",
            greetingWish = "तथागत गौतम बुद्ध यांच्या पावन जयंतीनिमित्त सर्व मानवजातीला शांती व समतेच्या हार्दिक शुभेच्छा!",
            emojiBadge = "☸️",
            gradientColors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFFFF176)),
            drawableRes = R.drawable.ic_fest_dharmachakra
        ),

        // ६. जून
        FestivalItem(
            id = "paryavaran_din",
            title = "जागतिक पर्यावरण दिन",
            subtitle = "॥ झाडे लावा, झाडे जगवा • वसुंधरा रक्षण ॥",
            greetingWish = "जागतिक पर्यावरण दिनाच्या शुभेच्छा! पर्यावरण रक्षणाचा संकल्प करूया, गाव सुंदर व हरित करूया.",
            emojiBadge = "🌱",
            gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF4CAF50)),
            drawableRes = R.drawable.ic_fest_knowledge,
            isNational = true
        ),
        FestivalItem(
            id = "vat_pournima",
            title = "वटपौर्णिमा",
            subtitle = "॥ अखंड सौभाग्याचे पावन व्रत ॥",
            greetingWish = "वटपौर्णिमेच्या पावन सणाच्या सर्व माता-भगिनींना हार्दिक व मंगलमय शुभेच्छा!",
            emojiBadge = "🌳",
            gradientColors = listOf(Color(0xFF2E7D32), Color(0xFFD81B60), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "yoga_din",
            title = "आंतरराष्ट्रीय योग दिन",
            subtitle = "॥ योगः कर्मसु कौशलम् • निरोगी जीवन ॥",
            gradientColors = listOf(Color(0xFF00695C), Color(0xFF00897B), GoldenTertiary),
            greetingWish = "आंतरराष्ट्रीय योग दिनाच्या हार्दिक शुभेच्छा! दररोज नियमित योग करा आणि निरोगी, सुदृढ राहा.",
            emojiBadge = "🧘",
            drawableRes = R.drawable.ic_fest_knowledge,
            isNational = true
        ),
        FestivalItem(
            id = "shahu_maharaj_jayanti",
            title = "राजर्षी छत्रपती शाहू महाराज जयंती",
            subtitle = "॥ सामाजिक न्यायाचे जनक • लोकराजे ॥",
            greetingWish = "आरक्षणाचे जनक, सामाजिक समतेचे महामेरू राजर्षी शाहू महाराज यांच्या जयंतीनिमित्त सामाजिक न्याय दिनी विनम्र अभिवादन!",
            emojiBadge = "👑",
            gradientColors = listOf(SaffronDark, Color(0xFFC2185B), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),

        // ७. जुलै
        FestivalItem(
            id = "ashadhi_ekadashi",
            title = "आषाढी एकादशी (पंढरपूर यात्रा)",
            subtitle = "॥ रूप पाहता लोचनी, सुख झाले वो साजणी ॥",
            greetingWish = "माऊली विठ्ठलाच्या पावन आषाढी एकादशीच्या सर्व विठ्ठलभक्तांना व वारकऱ्यांना भक्तीमय शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(Color(0xFFBF360C), Color(0xFFE65100), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_vithal
        ),
        FestivalItem(
            id = "guru_pournima",
            title = "गुरु पौर्णिमा",
            subtitle = "॥ गुरुर्ब्रह्मा गुरुर्विष्णुः गुरुर्देवो महेश्वरः ॥",
            greetingWish = "आयुष्याला योग्य दिशा दाखवणाऱ्या सर्व पूजनीय गुरुजनांना गुरुपौर्णिमेनिमित्त शतशः वंदन!",
            emojiBadge = "🙏",
            gradientColors = listOf(SaffronPrimary, Color(0xFFFFA000), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "karnataki_bendur",
            title = "कर्नाटकी बेंदूर",
            subtitle = "॥ शेतकरी राजा आणि लाडकी बैलजोडी ॥",
            greetingWish = "सीमाभागातील लाडक्या कर्नाटकी बेंदूर सणाच्या सर्व शेतकरी बांधवांना मनःपूर्वक हार्दिक शुभेच्छा!",
            emojiBadge = "🐂",
            gradientColors = listOf(Color(0xFFBF360C), SaffronDark, GoldenTertiary),
            drawableRes = R.drawable.ic_fest_bendur
        ),
        FestivalItem(
            id = "tilak_jayanti",
            title = "लोकमान्य बाळ गंगाधर टिळक जयंती",
            subtitle = "॥ स्वराज्य हा माझा जन्मसिद्ध हक्क आहे ॥",
            greetingWish = "भारतीय असंतोषाचे जनक, थोर विचारवंत लोकमान्य टिळक यांच्या जयंतीनिमित्त विनम्र अभिवादन!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronDark, Color(0xFFBF360C), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),

        // ८. ऑगस्ट
        FestivalItem(
            id = "annabhau_sathe_jayanti",
            title = "लोकशाहीर अण्णाभाऊ साठे जयंती",
            subtitle = "॥ जग बदल घालुनी घाव, सांगून गेले मज भीमराव ॥",
            greetingWish = "महान साहित्यिक, संयुक्त महाराष्ट्र चळवळीचे शिल्पकार लोकशाहीर अण्णाभाऊ साठे यांच्या जयंतीनिमित्त विनम्र अभिवादन!",
            emojiBadge = "✍️",
            gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "nana_patil_jayanti",
            title = "क्रांतिसिंह नाना पाटील जयंती",
            subtitle = "॥ प्रतिसरकारचे जनक • स्वातंत्र्ययोद्धा ॥",
            greetingWish = "प्रतिसरकारचे संस्थापक, थोर स्वातंत्र्यसेनानी क्रांतिसिंह नाना पाटील यांच्या जयंतीनिमित्त मानाचा मुजरा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronPrimary, Color(0xFFD84315), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "nag_panchami",
            title = "नागपंचमी",
            subtitle = "॥ निसर्गाशी कृतज्ञता • नागदेवता पूजन ॥",
            greetingWish = "श्रावण महिन्यातील पहिल्या सणाच्या म्हणजेच नागपंचमीच्या सर्व ग्रामस्थांना हार्दिक शुभेच्छा!",
            emojiBadge = "🐍",
            gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF388E3C), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "narali_pournima_rakshabandhan",
            title = "नारळी पौर्णिमा व रक्षाबंधन",
            subtitle = "॥ भाऊ-बहिणीच्या अतूट प्रेमाचा पवित्र सण ॥",
            greetingWish = "रक्षाबंधन व नारळी पौर्णिमेच्या सर्व बंधू-भगिनींना मंगलमय शुभेच्छा! नात्यातील गोडवा सदैव टिकून राहो.",
            emojiBadge = "🧵",
            gradientColors = listOf(Color(0xFFC2185B), Color(0xFFE91E63), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "independence_day",
            title = "भारतीय स्वातंत्र्य दिन",
            subtitle = "॥ जय हिंद, जय भारत • वंदे मातरम् ॥",
            greetingWish = "भारतीय स्वातंत्र्य दिनाच्या सर्व अर्जुनवाड ग्रामस्थांना व देशवासियांना मनःपूर्वक हार्दिक शुभेच्छा!",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(Color(0xFFFF9933), NavySecondary, Color(0xFF138808)),
            drawableRes = R.drawable.ic_tiranga_independence,
            isNational = true
        ),
        FestivalItem(
            id = "krishna_janmashtami",
            title = "श्रीकृष्ण जन्माष्टमी • गोकुळाष्टमी",
            subtitle = "॥ यदा यदा हि धर्मस्य ग्लानिर्भवति भारत ॥",
            greetingWish = "पूर्णपुरुषोत्तम बाळकृष्णाच्या पावन जन्मोत्सवाच्या सर्व भाविकांना भक्तीमय शुभेच्छा!",
            emojiBadge = "🪈",
            gradientColors = listOf(Color(0xFF1A237E), Color(0xFF283593), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_krishna
        ),
        FestivalItem(
            id = "dahi_handi",
            title = "गोपाळकाला • दहीहंडी महोत्सव",
            subtitle = "॥ गोविंदा रे गोपाळा • जल्लोष तरुणाईचा ॥",
            greetingWish = "दहीहंडी व गोपाळकाला महोत्सवाच्या सर्व गोविंदा पथकांना व क्रीडाप्रेमींना हार्दिक शुभेच्छा!",
            emojiBadge = "🏺",
            gradientColors = listOf(Color(0xFFD84315), Color(0xFFFFA000), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_krishna
        ),
        FestivalItem(
            id = "maharashtrian_bendur",
            title = "महाराष्ट्रीयन बेंदूर / पोळा",
            subtitle = "॥ बळीराजाचा सखा, लाडका सर्जा-राजा ॥",
            greetingWish = "सर्व शेतकरी बांधवांना आणि गावगाड्याला बेंदूर/बैलपोळ्याच्या मनःपूर्वक हार्दिक शुभेच्छा!",
            emojiBadge = "🐂",
            gradientColors = listOf(Color(0xFFBF360C), SaffronDark, GoldenTertiary),
            drawableRes = R.drawable.ic_fest_bendur
        ),
        FestivalItem(
            id = "national_sports_day",
            title = "राष्ट्रीय क्रीडा दिन",
            subtitle = "॥ मेजर ध्यानचंद जयंती • मैदानी खेळ, निरोगी देश ॥",
            greetingWish = "हॉकीचे जादूगार मेजर ध्यानचंद यांच्या जयंतीनिमित्त राष्ट्रीय क्रीडा दिनाच्या सर्व खेळाडूंना शुभेच्छा!",
            emojiBadge = "🏆",
            gradientColors = listOf(Color(0xFF00695C), Color(0xFF00897B), Color(0xFF4DB6AC)),
            drawableRes = R.drawable.ic_fest_knowledge,
            isNational = true
        ),

        // ९. सप्टेंबर
        FestivalItem(
            id = "teachers_day",
            title = "शिक्षक दिन",
            subtitle = "॥ डॉ. सर्वपल्ली राधाकृष्णन जयंती ॥",
            greetingWish = "विद्यार्थ्यांच्या जीवनाला आकार देणाऱ्या सर्व आदरणीय शिक्षकांना शिक्षक दिनानिमित्त विनम्र वंदन!",
            emojiBadge = "📚",
            gradientColors = listOf(Color(0xFF1565C0), Color(0xFF1E88E5), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge,
            isNational = true
        ),
        FestivalItem(
            id = "ganesh_chaturthi",
            title = "श्री गणेश चतुर्थी व गणेशोत्सव",
            subtitle = "॥ गणपती बाप्पा मोरया, मंगलमूर्ती मोरया ॥",
            greetingWish = "विघ्नहर्ता बाप्पाच्या आगमनानिमित्त समस्त अर्जुनवाड ग्रामस्थांना मंगलमय व भक्तीमय शुभेच्छा!",
            emojiBadge = "🌺",
            gradientColors = listOf(SaffronPrimary, Color(0xFFDC2626), GoldenTertiary),
            drawableRes = R.drawable.ic_ganesh_festival
        ),
        FestivalItem(
            id = "gauri_pujan",
            title = "ज्येष्ठ गौरी आवाहन व पूजन",
            subtitle = "॥ गौरी-गणपतीच्या आगमनाने घराघरात आनंद ॥",
            greetingWish = "महालक्ष्मी स्वरूप गौरी पूजनाच्या सर्व माता-भगिनींना आणि कुटुंबांना हार्दिक शुभेच्छा!",
            emojiBadge = "🌸",
            gradientColors = listOf(Color(0xFFAD1457), Color(0xFFC2185B), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "anant_chaturdashi",
            title = "अनंत चतुर्दशी",
            subtitle = "॥ पुढच्या वर्षी लवकर या • बाप्पाला भावपूर्ण निरोप ॥",
            greetingWish = "अनंत चतुर्दशीच्या सर्वांना हार्दिक शुभेच्छा! बाप्पा सर्वांच्या मनोकामना पूर्ण करो हीच प्रार्थना.",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronDark, Color(0xFFB91C1C), GoldenTertiary),
            drawableRes = R.drawable.ic_ganesh_festival
        ),

        // १०. ऑक्टोबर
        FestivalItem(
            id = "gandhi_jayanti",
            title = "महात्मा गांधी व लाल बहादूर शास्त्री जयंती",
            subtitle = "॥ सत्य, अहिंसा आणि जय जवान जय किसान ॥",
            greetingWish = "राष्ट्रपिता महात्मा गांधी व माजी पंतप्रधान लाल बहादूर शास्त्री यांच्या जयंतीनिमित्त विनम्र अभिवादन!",
            emojiBadge = "🕊️",
            gradientColors = listOf(Color(0xFF37474F), Color(0xFF455A64), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_gandhi,
            isNational = true
        ),
        FestivalItem(
            id = "navratri",
            title = "शारदीय नवरात्रोत्सव प्रारंभ",
            subtitle = "॥ उदो बोला उदो, अंबाबाई माऊलीचा हो ॥",
            greetingWish = "नवरात्रोत्सवाच्या व घटस्थापनेच्या सर्व भाविकांना मंगलमय शुभेच्छा! आदिशक्ती सर्वांचे रक्षण करो.",
            emojiBadge = "🪔",
            gradientColors = listOf(Color(0xFFB71C1C), Color(0xFFC62828), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "durga_ashtami",
            title = "दुर्गाष्टमी",
            subtitle = "॥ जय दुर्गे दुर्घट भारी संकटी संकष्टी ॥",
            greetingWish = "महाष्टमी व दुर्गाष्टमीच्या पावन तिथीच्या सर्व देवीभक्तांना मनःपूर्वक भक्तीमय शुभेच्छा!",
            emojiBadge = "🌺",
            gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "dussehra",
            title = "विजयादशमी • दसरा",
            subtitle = "॥ सोने घ्या, सोन्यासारखे राहा ॥",
            greetingWish = "अनिष्टावर इष्टाच्या विजयाचे प्रतीक असणाऱ्या दसऱ्याच्या सर्व ग्रामस्थांना सुवर्णमय शुभेच्छा!",
            emojiBadge = "🏹",
            gradientColors = listOf(SaffronDark, Color(0xFFB45309), GoldenTertiary),
            drawableRes = R.drawable.ic_shri_ram_festival
        ),
        FestivalItem(
            id = "kojagiri_pournima",
            title = "कोजागिरी पौर्णिमा",
            subtitle = "॥ को जागर्ति • दुग्धशर्करा योग ॥",
            greetingWish = "शरद पौर्णिमेच्या आणि कोजागिरीच्या सर्व परिवारांना मधुर व आनंदाच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🌕",
            gradientColors = listOf(Color(0xFF283593), Color(0xFF3949AB), Color(0xFFFFF9C4)),
            drawableRes = R.drawable.ic_fest_krishna
        ),
        FestivalItem(
            id = "sardar_patel_jayanti",
            title = "सरदार वल्लभभाई पटेल जयंती • राष्ट्रीय एकता दिन",
            subtitle = "॥ भारताचे लोहपुरुष • अखंड भारताचे शिल्पकार ॥",
            greetingWish = "लोहपुरुष सरदार वल्लभभाई पटेल यांच्या जयंतीनिमित्त व राष्ट्रीय एकता दिनी शतशः नमन!",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(SaffronPrimary, NavySecondary, Color(0xFF1B5E20)),
            drawableRes = R.drawable.ic_tiranga_independence,
            isNational = true
        ),

        // ११. नोव्हेंबर (दिवाळी पर्व)
        FestivalItem(
            id = "dhanatrayodashi",
            title = "धनत्रयोदशी • धन्वंतरी जयंती",
            subtitle = "॥ उत्तम आरोग्य आणि समृद्धी ॥",
            greetingWish = "धनत्रयोदशी व धन्वंतरी पूजनाच्या हार्दिक शुभेच्छा! सर्वांना उत्तम आरोग्य व धनधान्य लाभो.",
            emojiBadge = "🪔",
            gradientColors = listOf(Color(0xFFF57F17), Color(0xFFFFA000), GoldenTertiary),
            drawableRes = R.drawable.ic_diwali_deep
        ),
        FestivalItem(
            id = "narak_chaturdashi",
            title = "नरक चतुर्दशी • अभ्यंगस्नान",
            subtitle = "॥ मांगल्याची पहाट, सुगंधी उटणे ॥",
            greetingWish = "नरक चतुर्दशीच्या व पहिल्या अभ्यंगस्नानाच्या सर्व सभासदांना मंगलमय शुभेच्छा!",
            emojiBadge = "✨",
            gradientColors = listOf(Color(0xFFE65100), Color(0xFFFF6F00), GoldenTertiary),
            drawableRes = R.drawable.ic_diwali_deep
        ),
        FestivalItem(
            id = "laxmi_pujan",
            title = "दीपावली • श्री लक्ष्मीपूजन",
            subtitle = "॥ ॐ महालक्ष्म्यै च विद्महे • सुख-समृद्धी लाभो ॥",
            greetingWish = "शुभ दीपावली व लक्ष्मीपूजनाच्या सर्व अर्जुनवाड ग्रामस्थांना दीपमय हार्दिक शुभेच्छा!",
            emojiBadge = "🪔",
            gradientColors = listOf(SaffronDark, Color(0xFFB45309), GoldenTertiary),
            drawableRes = R.drawable.ic_diwali_deep
        ),
        FestivalItem(
            id = "diwali_padwa",
            title = "दिवाळी पाडवा • बलिप्रतिपदा",
            subtitle = "॥ पती-पत्नीच्या स्नेहाचे पावन पर्व ॥",
            greetingWish = "दिवाळी पाडवा व बलिप्रतिपदेच्या सर्व सभासदांना व त्यांच्या कुटुंबांना हार्दिक शुभेच्छा!",
            emojiBadge = "🎁",
            gradientColors = listOf(SaffronPrimary, Color(0xFFE65100), GoldenTertiary),
            drawableRes = R.drawable.ic_diwali_deep
        ),
        FestivalItem(
            id = "bhaubeej",
            title = "भाऊबीज",
            subtitle = "॥ बहिणीची प्रार्थना, भावाची साथ ॥",
            greetingWish = "भाऊबीजेच्या पवित्र सणाच्या सर्व बंधू-भगिनींना प्रेमपूर्वक मनःपूर्वक शुभेच्छा!",
            emojiBadge = "❤️",
            gradientColors = listOf(Color(0xFFAD1457), Color(0xFFC2185B), GoldenTertiary),
            drawableRes = R.drawable.ic_diwali_deep
        ),
        FestivalItem(
            id = "nehru_jayanti_childrens_day",
            title = "पं. जवाहरलाल नेहरू जयंती • बालदिन",
            subtitle = "॥ चाचा नेहरू • देशाचे भविष्य बालके ॥",
            greetingWish = "भारताचे पहिले पंतप्रधान पं. नेहरू यांच्या जयंतीनिमित्त सर्व चिमुकल्यांना बालदिनाच्या शुभेच्छा!",
            emojiBadge = "🎈",
            gradientColors = listOf(Color(0xFF0288D1), Color(0xFF03A9F4), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge,
            isNational = true
        ),
        FestivalItem(
            id = "tulsi_vivah",
            title = "तुळशी विवाह प्रारंभ",
            subtitle = "॥ मंगल कार्यांची नांदी ॥",
            greetingWish = "तुळशी विवाहाच्या सर्व ग्रामस्थांना मंगलमय शुभेच्छा! घरात सदैव सुख-समाधान राहो.",
            emojiBadge = "🌿",
            gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "kartiki_ekadashi",
            title = "कार्तिकी एकादशी • प्रबोधिनी एकादशी",
            subtitle = "॥ हरी जागृती उत्सव • पंढरीची वारी ॥",
            greetingWish = "कार्तिकी एकादशीच्या सर्व वारकऱ्यांना व विठ्ठलभक्तांना मनःपूर्वक भक्तीमय शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(Color(0xFFBF360C), Color(0xFFE65100), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_vithal
        ),
        FestivalItem(
            id = "sanvidhan_din",
            title = "भारतीय संविधान दिन",
            subtitle = "॥ आम्ही भारताचे लोक... • लोकशाहीचा गौरव ॥",
            greetingWish = "२६ नोव्हेंबर भारतीय संविधान दिनाच्या सर्व देशवासियांना हार्दिक शुभेच्छा! संविधानाचा सन्मान करूया.",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(NavySecondary, Color(0xFF1565C0), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_national_emblem,
            isNational = true
        ),

        // १२. डिसेंबर
        FestivalItem(
            id = "datta_jayanti",
            title = "श्री गुरुदेव दत्त जयंती",
            subtitle = "॥ दिगंबरा दिगंबरा श्रीपाद वल्लभ दिगंबरा ॥",
            greetingWish = "त्रिमूर्ती स्वरूप भगवान दत्तात्रेयांच्या पावन जन्मोत्सवाच्या सर्व दत्तभक्तांना भक्तीमय शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronDark, Color(0xFFD97706), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_vithal
        ),
        FestivalItem(
            id = "christmas",
            title = "नाताळ (ख्रिसमस)",
            subtitle = "॥ शांती, करुणा व आनंदाचा सण ॥",
            greetingWish = "नाताळच्या (Merry Christmas) सर्व बांधवांना आनंददायी व सुखसमृद्धीच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🎄",
            gradientColors = listOf(Color(0xFFB71C1C), Color(0xFF1B5E20), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),

        // १३. सर्वधर्मसमभाव व इतर विशेष उत्सव
        FestivalItem(
            id = "eid_ul_fitr",
            title = "ईद-उल-फितर (रमजान ईद)",
            subtitle = "॥ ईद मुबारक • बंधुभाव व सलोखा ॥",
            greetingWish = "ईद-उल-फितरच्या सर्व मुस्लिम बांधवांना व ग्रामस्थांना मनापासून 'ईद मुबारक'!",
            emojiBadge = "🌙",
            gradientColors = listOf(Color(0xFF004D40), Color(0xFF00695C), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_crescent
        ),
        FestivalItem(
            id = "eid_ul_adha",
            title = "ईद-उल-अजहा (बकरी ईद)",
            subtitle = "॥ त्याग आणि समर्पणाचा सण ॥",
            greetingWish = "ईद-उल-अजहाच्या सर्व बांधवांना सुख, शांती आणि समृद्धीच्या मनःपूर्वक शुभेच्छा!",
            emojiBadge = "🌙",
            gradientColors = listOf(Color(0xFF004D40), Color(0xFF00796B), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_crescent
        ),
        FestivalItem(
            id = "guru_nanak_jayanti",
            title = "गुरु नानक देव जयंती • प्रकाश पर्व",
            subtitle = "॥ वाहेगुरु जी का खालसा, वाहेगुरु जी की फतेह ॥",
            greetingWish = "शीख धर्माचे संस्थापक गुरु नानक देव जी यांच्या पावन प्रकाश पर्वाच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🪔",
            gradientColors = listOf(Color(0xFFE65100), Color(0xFFFFA000), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "good_friday",
            title = "गुड फ्रायडे",
            subtitle = "॥ प्रभू येशू ख्रिस्तांचे सर्वोच्च बलिदान व क्षमा ॥",
            greetingWish = "गुड फ्रायडे निमित्त प्रभू येशूंच्या शांती, करुणा व प्रेमाच्या संदेशाचे स्मरण करूया.",
            emojiBadge = "✝️",
            gradientColors = listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF455A64)),
            drawableRes = R.drawable.ic_fest_knowledge
        ),
        FestivalItem(
            id = "parsi_new_year",
            title = "पारशी नववर्ष (नवरोझ)",
            subtitle = "॥ नवरोझ मुबारक ॥",
            greetingWish = "पारशी नववर्षानिमित्त (नवरोझ) सर्वांना आनंद, भरभराट व उत्तम आरोग्याच्या मनःपूर्वक शुभेच्छा!",
            emojiBadge = "🌸",
            gradientColors = listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA), GoldenTertiary),
            drawableRes = R.drawable.ic_fest_kalash
        ),
        FestivalItem(
            id = "sankashti_angarki",
            title = "संकष्टी / अंगारकी संकष्टी चतुर्थी",
            subtitle = "॥ विघ्नहर्ता बाप्पा संकटे दूर करो ॥",
            greetingWish = "संकष्टी चतुर्थीच्या सर्व गणेशभक्तांना भक्तीमय शुभेच्छा! गणपती बाप्पा सर्वांचे कल्याण करो.",
            emojiBadge = "🌺",
            gradientColors = listOf(SaffronPrimary, Color(0xFFC2185B), GoldenTertiary),
            drawableRes = R.drawable.ic_ganesh_festival
        )
    )

    /**
     * कॅलेंडर तारखेनुसार आज कोणता सण आहे का ते तपासणे (Fixed Gregorian Dates + Tithi Mappings)
     * जर आज कोणताही सण नसेल तर null परत करेल.
     */
    fun findFestivalForCurrentDate(): FestivalItem? {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // १. कायमस्वरूपी ठरलेल्या तारखा (Gregorian Solar Dates)
        when {
            month == 1 && day in 3..4 -> return findById("savitribai_phule_jayanti")
            month == 1 && day in 11..13 -> return findById("swami_vivekanand_jayanti")
            month == 1 && day in 14..16 -> return findById("makar_sankrant")
            month == 1 && day == 23 -> return findById("netaji_subhash_bose_jayanti")
            month == 1 && day == 25 -> return findById("voters_day")
            month == 1 && day in 26..27 -> return findById("republic_day")
            month == 1 && day == 30 -> return findById("martyrs_day")

            month == 2 && day in 18..20 -> return findById("shivaji_maharaj_jayanti")
            month == 2 && day in 27..28 -> return findById("science_day")

            month == 4 && day in 10..12 -> return findById("mahatma_phule_jayanti")
            month == 4 && day in 13..15 -> return findById("dr_ambedkar_jayanti")

            month == 5 && day in 1..2 -> return findById("maharashtra_din")

            month == 6 && day == 5 -> return findById("paryavaran_din")
            month == 6 && day == 21 -> return findById("yoga_din")
            month == 6 && day in 25..27 -> return findById("shahu_maharaj_jayanti")

            month == 7 && day == 23 -> return findById("tilak_jayanti")

            month == 8 && day == 1 -> return findById("annabhau_sathe_jayanti")
            month == 8 && day == 3 -> return findById("nana_patil_jayanti")
            month == 8 && day in 14..16 -> return findById("independence_day")
            month == 8 && day == 29 -> return findById("national_sports_day")

            month == 9 && day in 4..6 -> return findById("teachers_day")

            month == 10 && day in 1..3 -> return findById("gandhi_jayanti")
            month == 10 && day == 31 -> return findById("sardar_patel_jayanti")

            month == 11 && day == 14 -> return findById("nehru_jayanti_childrens_day")
            month == 11 && day in 25..27 -> return findById("sanvidhan_din")

            month == 12 && day in 24..26 -> return findById("christmas")
        }

        // २. २०२६ मधील तिथीनुसार सण (Year 2026 Lunar Festivals Mapping)
        if (year == 2026) {
            when {
                month == 1 && day in 22..24 -> return findById("vasant_panchami")
                month == 2 && day in 15..17 -> return findById("mahashivratri")
                month == 3 && day in 2..4 -> return findById("holi")
                month == 3 && day in 5..7 -> return findById("dhulivandan")
                month == 3 && day in 14..16 -> return findById("sant_tukaram_beej")
                month == 3 && day in 18..21 -> return findById("gudi_padwa")
                month == 3 && day in 27..29 -> return findById("shri_ram_navami")
                month == 4 && day in 1..3 -> return findById("hanuman_jayanti")
                month == 4 && day in 18..21 -> return findById("akshaya_tritiya")
                month == 5 && day in 30..31 || (month == 6 && day == 1) -> return findById("vat_pournima")
                month == 6 && day in 25..27 -> return findById("ashadhi_ekadashi")
                month == 7 && day in 11..13 -> return findById("karnataki_bendur")
                month == 7 && day in 28..30 -> return findById("guru_pournima")
                month == 8 && day in 17..19 -> return findById("nag_panchami")
                month == 8 && day in 27..29 -> return findById("narali_pournima_rakshabandhan")
                month == 9 && day in 3..5 -> return findById("krishna_janmashtami")
                month == 9 && day in 6..7 -> return findById("dahi_handi")
                month == 9 && day in 11..13 -> return findById("maharashtrian_bendur")
                month == 9 && day in 13..24 -> return findById("ganesh_chaturthi")
                month == 9 && day in 18..20 -> return findById("gauri_pujan")
                month == 9 && day in 24..26 -> return findById("anant_chaturdashi")
                month == 10 && day in 11..20 -> return findById("navratri")
                month == 10 && day in 18..19 -> return findById("durga_ashtami")
                month == 10 && day in 20..22 -> return findById("dussehra")
                month == 10 && day in 25..27 -> return findById("kojagiri_pournima")
                month == 11 && day in 6..8 -> return findById("dhanatrayodashi")
                month == 11 && day in 8..9 -> return findById("narak_chaturdashi")
                month == 11 && day in 9..10 -> return findById("laxmi_pujan")
                month == 11 && day in 10..11 -> return findById("diwali_padwa")
                month == 11 && day in 11..12 -> return findById("bhaubeej")
                month == 11 && day in 20..22 -> return findById("tulsi_vivah")
                month == 11 && day in 20..22 -> return findById("kartiki_ekadashi")
                month == 12 && day in 23..25 -> return findById("datta_jayanti")
            }
        }

        return null
    }

    fun findById(id: String): FestivalItem? {
        return ALL_FESTIVALS.find { it.id == id }
    }
}
