package com.example.data.sample

import com.example.data.model.ClassificationItem
import com.example.data.model.ClinicalPoint
import com.example.data.model.ExamEmphasisItem
import com.example.data.model.ManagementItem
import com.example.data.model.MedicalLectureNote
import com.example.data.model.NumberDoseItem
import com.example.data.model.QuestionAnswerItem

object SampleLectures {

    val heartFailureLecture = MedicalLectureNote(
        id = 1,
        title = "Cardiology: HFrEF & Guideline-Directed Medical Therapy",
        specialty = "Cardiology",
        createdAt = System.currentTimeMillis() - 86400000 * 2,
        audioDurationSeconds = 2740,
        summary = "Comprehensive lecture on Heart Failure with Reduced Ejection Fraction (HFrEF, LVEF ≤ 40%), covering neurohormonal activation, the 'Four Pillars' of Guideline-Directed Medical Therapy (GDMT), exact hemodynamic cutoffs, and landmark clinical trial pearls for USMLE/board exams.",
        coreConcepts = listOf(
            "Neurohormonal Maladaptation: Chronic sympathetic nervous system (SNS) and renin-angiotensin-aldosterone system (RAAS) overactivation leads to adverse ventricular remodeling, interstitial fibrosis, and apoptosis.",
            "Frank-Starling Law Failure: In HFrEF, the left ventricle shifts downward and rightward on the ventricular function curve; volume loading increases capillary wedge pressure without improving stroke volume.",
            "Neprilysin Inhibition Mechanism: Sacubitril inhibits neprilysin (which degrades ANP, BNP, bradykinin, and adrenomedullin), augmenting natriuresis and vasodilation, but must always be paired with an ARB (Valsartan) to block AT1 receptors."
        ),
        importantPoints = listOf(
            "All four foundational pillars of GDMT should be initiated simultaneously or in rapid sequence within 2 to 4 weeks of diagnosis.",
            "Do NOT initiate Beta-Blockers during acute decompensation with volume overload; stabilize patient with loop diuretics first, then titrate low and slow.",
            "SGLT2 inhibitors (Dapagliflozin / Empagliflozin) reduce cardiovascular death and HF hospitalization regardless of whether the patient has diabetes mellitus."
        ),
        classifications = listOf(
            ClassificationItem(
                title = "NYHA Functional Classification",
                category = "Symptomatic Severity",
                criteria = listOf(
                    "Class I: No limitation of physical activity. Ordinary physical activity does not cause undue fatigue or dyspnea.",
                    "Class II: Slight limitation. Comfortable at rest, but ordinary activity results in fatigue, palpitations, or dyspnea.",
                    "Class III: Marked limitation. Comfortable at rest, but less than ordinary activity causes symptoms.",
                    "Class IV: Inability to carry on any physical activity without discomfort; symptoms present even at rest."
                )
            ),
            ClassificationItem(
                title = "ACC/AHA Heart Failure Stages",
                category = "Disease Progression",
                criteria = listOf(
                    "Stage A: At risk for HF without structural heart disease or symptoms (e.g. HTN, DM, cardiotoxic chemo).",
                    "Stage B: Pre-HF; structural heart disease present (e.g. prior MI, LVH, reduced EF) but NO symptoms.",
                    "Stage C: Structural heart disease with current or prior symptoms of HF.",
                    "Stage D: Refractory HF requiring specialized interventions (LVAD, inotropes, cardiac transplant)."
                )
            )
        ),
        clinicalPoints = listOf(
            ClinicalPoint(
                category = "Presentation",
                description = "Paroxysmal nocturnal dyspnea (PND), orthopnea requiring 3+ pillows, exertional dyspnea, and peripheral pitting edema.",
                isHighYield = true
            ),
            ClinicalPoint(
                category = "Physical Exam",
                description = "Elevated Jugular Venous Distension (JVD > 8 cm H2O), hepatojugular reflux, displaced hyperdynamic apex beat, lateralized apical impulse, and S3 gallop (volume overload sign).",
                isHighYield = true
            ),
            ClinicalPoint(
                category = "Diagnostics",
                description = "Transthoracic Echocardiography (TTE) is mandatory to calculate LVEF, ventricular dimensions, wall motion abnormalities, and valvular function.",
                isHighYield = true
            ),
            ClinicalPoint(
                category = "Lab Cutoffs",
                description = "Serum NT-proBNP > 300 pg/mL (acute) or BNP > 100 pg/mL strongly supports diagnosis; caution: BNP is falsely elevated in renal failure and falsely lowered in severe obesity.",
                isHighYield = true
            )
        ),
        management = listOf(
            ManagementItem(
                line = "First-Line (Pillar 1)",
                intervention = "ARNI (Sacubitril/Valsartan) or ACEi/ARB",
                rationale = "Replaces ACEi/ARB to further reduce mortality; requires mandatory 36-hour washout if switching from ACEi to avoid fatal angioedema."
            ),
            ManagementItem(
                line = "First-Line (Pillar 2)",
                intervention = "Evidence-Based Beta-Blocker (Carvedilol, Metoprolol Succinate, or Bisoprolol)",
                rationale = "Blocks sympathetic cardiotoxicity; mortality reduction demonstrated ONLY with these 3 specific agents (not atenolol or metoprolol tartrate)."
            ),
            ManagementItem(
                line = "First-Line (Pillar 3)",
                intervention = "Mineralocorticoid Receptor Antagonist (MRA: Spironolactone 25-50 mg or Eplerenone)",
                rationale = "Reduces myocardial fibrosis and sudden cardiac death; check K+ and creatinine within 1-2 weeks."
            ),
            ManagementItem(
                line = "First-Line (Pillar 4)",
                intervention = "SGLT2 Inhibitor (Dapagliflozin 10 mg or Empagliflozin 10 mg QD)",
                rationale = "Promotes osmotic diuresis, decreases intraglomerular pressure, improves myocardial bioenergetics."
            ),
            ManagementItem(
                line = "Symptomatic",
                intervention = "Loop Diuretics (Furosemide 20-40 mg IV/oral, Bumetanide, or Torsemide)",
                rationale = "Relieves congestive symptoms and pulmonary rales; does NOT reduce mortality on its own."
            ),
            ManagementItem(
                line = "Contraindication",
                intervention = "Non-Dihydropyridine Calcium Channel Blockers (Diltiazem, Verapamil) and NSAIDs",
                rationale = "Negative inotropic effect precipitates acute cardiogenic shock; NSAIDs worsen renal perfusion and induce sodium retention."
            )
        ),
        numbersAndDoses = listOf(
            NumberDoseItem(
                item = "HFrEF diagnostic threshold",
                exactValue = "LVEF ≤ 40%",
                context = "Left Ventricular Ejection Fraction defined by echocardiography.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "ACEi to ARNI Washout Period",
                exactValue = "36 hours",
                context = "Mandatory waiting interval to prevent severe bradykinin-mediated angioedema.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Sacubitril/Valsartan starting dose",
                exactValue = "49/51 mg PO BID",
                context = "Standard starting dose for patients previously on standard-dose ACEi/ARB; titrated to 97/103 mg PO BID.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Spironolactone eGFR and Potassium safety limits",
                exactValue = "eGFR > 30 mL/min/1.73m² and Serum K+ < 5.0 mEq/L",
                context = "Contraindicated if hyperkalemic or severe renal insufficiency.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Target resting heart rate on Beta-Blocker",
                exactValue = "55 – 60 bpm",
                context = "Titration target provided by lecturer; if resting HR remains > 70 bpm in sinus rhythm despite max BB, add Ivabradine.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Hydralazine + Isosorbide Dinitrate dose in African American patients",
                exactValue = "37.5 mg / 20 mg PO TID",
                context = "[unclear] titration schedule mentioned in lecture; verify local institution protocol.",
                isUnclear = true
            )
        ),
        examEmphasis = listOf(
            ExamEmphasisItem(
                topic = "S3 Gallop Origin",
                buzzword = "Rapid passive ventricular filling into a stiff/dilated noncompliant ventricle",
                pearl = "Best heard at cardiac apex in left lateral decubitus position with bell of stethoscope at end-expiration.",
                trapOrWarning = "Do not confuse with S4, which is active atrial kick against stiff hypertrophic ventricle."
            ),
            ExamEmphasisItem(
                topic = "Pharmacology Trap: Metoprolol Tartrate vs Succinate",
                buzzword = "Toprol-XL (Succinate) reduces mortality; Lopressor (Tartrate) does NOT",
                pearl = "Exam boards frequently test this distractor: only Succinate once-daily has mortality benefit in HF.",
                trapOrWarning = "Selecting metoprolol tartrate on board exams is a classic zero-credit trap."
            ),
            ExamEmphasisItem(
                topic = "Chest X-Ray Findings in Pulmonary Edema",
                buzzword = "Kerley B lines (interlobular septal edema) and Bat-wing alveolar infiltrates",
                pearl = "Cardiothoracic ratio > 50% on PA view indicates cardiomegaly.",
                trapOrWarning = "AP portable views artificially magnify cardiac silhouette."
            )
        ),
        questionAnswers = listOf(
            QuestionAnswerItem(
                question = "Why must you wait 36 hours when switching a patient from Lisinopril to Sacubitril/Valsartan?",
                answer = "Because both ACE inhibitors and Neprilysin inhibitors prevent the enzymatic breakdown of bradykinin. Dual inhibition causes massive bradykinin accumulation leading to potentially fatal angioedema.",
                lecturerNote = "Prof emphasized this is a guaranteed board question."
            ),
            QuestionAnswerItem(
                question = "Can SGLT2 inhibitors be prescribed to a heart failure patient whose HbA1c is completely normal (5.4%)?",
                answer = "Yes! Clinical trials (DAPA-HF, EMPEROR-Reduced) proved mortality and hospital reduction benefits are independent of glycemic control or diabetes presence.",
                lecturerNote = "Lecturer highlighted the shift from diabetes drug to cardiovascular drug."
            ),
            QuestionAnswerItem(
                question = "When is an Implantable Cardioverter-Defibrillator (ICD) indicated for primary prevention in HFrEF?",
                answer = "In patients with NYHA Class II-III symptoms, LVEF ≤ 35% despite at least 3 months of optimal GDMT, and expected meaningful survival > 1 year.",
                lecturerNote = "Remember the 3-month medical therapy prerequisite before device placement."
            )
        ),
        highestYieldPoints = listOf(
            "The 4 Mortality-Reducing Pillars of HFrEF: ARNI, Beta-Blocker (Carvedilol/Metoprolol Succinate/Bisoprolol), MRA (Spironolactone), and SGLT2i (Dapagliflozin/Empagliflozin).",
            "Loop diuretics relieve congestion but do NOT improve overall survival.",
            "Never start or up-titrate Beta-Blockers during acute decompensation (cold and wet or warm and wet); achieve euvolemia first.",
            "Mandatory 36-hour washout between ACE inhibitor and ARNI to prevent life-threatening angioedema.",
            "Spironolactone requires baseline Serum K+ < 5.0 mEq/L and eGFR > 30 mL/min to prevent lethal hyperkalemia."
        ),
        isBookmarked = true
    )

    val strokeLecture = MedicalLectureNote(
        id = 2,
        title = "Neurology: Acute Ischemic Stroke & Thrombolytic Windows",
        specialty = "Neurology",
        createdAt = System.currentTimeMillis() - 86400000 * 5,
        audioDurationSeconds = 2100,
        summary = "Emergency management of Acute Ischemic Stroke (AIS), NIH Stroke Scale calculation, non-contrast head CT triage to rule out hemorrhage, exact intravenous Alteplase/Tenecteplase eligibility windows, permissive hypertension limits, and endovascular thrombectomy criteria.",
        coreConcepts = listOf(
            "Ischemic Core vs Penumbra: The core represents irreversibly infarcted necrotic tissue; the surrounding penumbra is hypoperfused, electrically silent but viable tissue that can be rescued by timely reperfusion.",
            "Excitotoxic Cascade: Energy depletion impairs Na+/K+ ATPase, leading to persistent membrane depolarization, unregulated glutamate release, massive intracellular calcium influx, and enzymatic self-digestion.",
            "CT Hypoattenuation Sign: Early subtle signs on non-contrast CT include loss of insular ribbon, obscuration of lentiform nucleus, and hyperdense MCA sign."
        ),
        importantPoints = listOf(
            "Time is Brain: 1.9 million neurons are lost every minute an untreated large vessel occlusion persists.",
            "First diagnostic priority upon arrival is immediate Non-Contrast Head CT to rule out Intracranial Hemorrhage (ICH).",
            "Permissive Hypertension: If not receiving IV thrombolysis, allow BP up to 220/120 mmHg to maintain penumbral collateral perfusion."
        ),
        classifications = listOf(
            ClassificationItem(
                title = "TOAST Stroke Subtypes",
                category = "Etiology",
                criteria = listOf(
                    "Large-artery atherosclerosis (carotid bifurcation, MCA stem stenosis)",
                    "Cardioembolism (Atrial Fibrillation, mural thrombus, prosthetic valve)",
                    "Small-vessel occlusion / Lacunar infarct (hypertensive lipohyalinosis)",
                    "Stroke of other determined etiology (arterial dissection, vasculitis, hypercoagulable state)",
                    "Stroke of undetermined etiology (cryptogenic or incomplete evaluation)"
                )
            )
        ),
        clinicalPoints = listOf(
            ClinicalPoint(
                category = "Presentation",
                description = "Sudden onset focal neurological deficit: facial droop, contralateral hemiparesis, hemisensory loss, aphasia (dominant hemisphere) or hemispatial neglect (non-dominant hemisphere).",
                isHighYield = true
            ),
            ClinicalPoint(
                category = "Diagnostics",
                description = "Immediate Non-Contrast Head CT within 20 minutes of door arrival; CTA head and neck to assess Large Vessel Occlusion (LVO) from aortic arch to circle of Willis.",
                isHighYield = true
            )
        ),
        management = listOf(
            ManagementItem(
                line = "First-Line Reperfusion",
                intervention = "IV Thrombolysis (Alteplase 0.9 mg/kg or Tenecteplase 0.25 mg/kg)",
                rationale = "Administered within 4.5 hours of Last Known Well (LKW) if no contraindications."
            ),
            ManagementItem(
                line = "Mechanical Reperfusion",
                intervention = "Endovascular Thrombectomy (EVT)",
                rationale = "Indicated within 6 hours (and up to 24 hours selected by perfusion mismatch on DAWN/DEFUSE-3 criteria) for anterior circulation LVO."
            ),
            ManagementItem(
                line = "Blood Pressure Control",
                intervention = "IV Labetalol or Nicardipine infusion",
                rationale = "Maintain BP < 185/110 mmHg prior to thrombolysis, and < 180/105 mmHg for 24 hours post-thrombolysis to prevent reperfusion hemorrhage."
            )
        ),
        numbersAndDoses = listOf(
            NumberDoseItem(
                item = "IV Alteplase Time Window",
                exactValue = "< 4.5 hours from Last Known Well",
                context = "Strict exclusion if waking up with symptoms unless advanced MRI mismatch (FLAIR negative, DWI positive) is confirmed.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "IV Alteplase standard dose",
                exactValue = "0.9 mg/kg (max 90 mg)",
                context = "10% given as initial IV bolus over 1 minute, remaining 90% infused over 60 minutes.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Pre-thrombolysis Blood Pressure limit",
                exactValue = "< 185/110 mmHg",
                context = "If BP exceeds this cutoff despite acute IV antihypertensives, thrombolytic therapy is contraindicated.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Permissive Hypertension limit (non-thrombolysis candidates)",
                exactValue = "< 220/120 mmHg",
                context = "Do not treat hypertension unless BP exceeds 220/120 mmHg or end-organ damage (aortic dissection, myocardial infarction) occurs.",
                isUnclear = false
            ),
            NumberDoseItem(
                item = "Aspirin initiation post-thrombolysis delay",
                exactValue = "24 hours",
                context = "Hold antiplatelets and anticoagulants for 24 hours post-tPA until repeat 24-hr CT rules out intracranial hemorrhage.",
                isUnclear = false
            )
        ),
        examEmphasis = listOf(
            ExamEmphasisItem(
                topic = "Blood Glucose Rule-Out",
                buzzword = "Always check fingerstick blood glucose immediately",
                pearl = "Severe hypoglycemia (< 60 mg/dL) can mimic dense hemiparesis and acute aphasia perfectly.",
                trapOrWarning = "Failing to check glucose before initiating thrombolytic protocol is an automatic fail."
            ),
            ExamEmphasisItem(
                topic = "Aphasia Localization: Broca vs Wernicke",
                buzzword = "Broca = Broken speech (non-fluent, comprehension intact); Wernicke = Word salad (fluent, impaired comprehension)",
                pearl = "Broca is inferior frontal gyrus (MCA superior division); Wernicke is superior temporal gyrus (MCA inferior division).",
                trapOrWarning = "Conduction aphasia has preserved comprehension and fluency but severely impaired repetition (arcuate fasciculus damage)."
            )
        ),
        questionAnswers = listOf(
            QuestionAnswerItem(
                question = "A patient wakes up at 7:00 AM with left arm paralysis. They went to sleep normal at 10:00 PM. What is their Last Known Well time?",
                answer = "10:00 PM the previous night. Last Known Well is the last time the patient was witnessed symptom-free, NOT the time they woke up.",
                lecturerNote = "This makes them outside the standard 4.5 hour tPA window unless perfusion MRI qualifies them."
            ),
            QuestionAnswerItem(
                question = "When should aspirin be administered to a stroke patient who just received IV Alteplase?",
                answer = "Hold aspirin for 24 hours post-infusion. Obtain a non-contrast CT at 24 hours to ensure no hemorrhagic transformation before giving aspirin 160-325 mg.",
                lecturerNote = "Classic board question testing antiplatelet safety post-fibrinolysis."
            )
        ),
        highestYieldPoints = listOf(
            "Immediate non-contrast head CT is required to differentiate ischemic stroke from hemorrhagic stroke.",
            "IV Alteplase window is 4.5 hours; maximum dose is 90 mg (0.9 mg/kg total; 10% bolus, 90% infusion over 1 hour).",
            "Strict blood pressure cutoffs: < 185/110 mmHg before tPA, < 180/105 mmHg for 24 hours after tPA.",
            "Permissive hypertension allows BP up to 220/120 mmHg in patients NOT receiving thrombolytics.",
            "Hold antiplatelets and anticoagulants for exactly 24 hours post-tPA until follow-up CT confirms absence of hemorrhage."
        ),
        isBookmarked = false
    )

    val sampleLecturesList = listOf(heartFailureLecture, strokeLecture)
}
