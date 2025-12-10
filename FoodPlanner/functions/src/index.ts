import {genkit, z} from "genkit";
import {firebase} from "@genkit-ai/firebase";
import {googleAI, gemini15Flash} from "@genkit-ai/googleai";
import * as admin from "firebase-admin";
import {onCall} from "firebase-functions/v2/https";
import * as functions from "firebase-functions";

// Inicializar Firebase Admin
admin.initializeApp();
const db = admin.firestore();

// 1. Inicializar Genkit (Nueva forma)
const ai = genkit({
  plugins: [firebase(), googleAI()],
  model: gemini15Flash, // Modelo por defecto
});

// 2. Definir el esquema de entrada (opcional, pero buena práctica)
// Comentado para evitar error de variable no usada
// const RecipeInputSchema = z.object({
//   question: z.string(),
// });

export const suggestRecipe = onCall(async (request) => {
  // Verificación de autenticación
  if (!request.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Debes iniciar sesión para pedir recetas."
    );
  }

  const userId = request.auth.uid;
  // Acceder a los datos de la solicitud de forma segura
  const userQuestion = request.data.question;

  // Leer inventario
  const inventorySnapshot = await db
    .collection("users")
    .doc(userId)
    .collection("inventory")
    .get();

  if (inventorySnapshot.empty) {
    return {text: "Tu inventario está vacío. ¡Añade ingredientes primero!"};
  }

  // Formatear inventario
  const inventoryList = inventorySnapshot.docs.map((doc) => {
    const data = doc.data();
    const expiry = data.expirationDate ?
      new Date(data.expirationDate).toLocaleDateString() :
      "sin fecha";
    return `- ${data.name} (${data.quantity} ${data.unit}), caduca: ${expiry}`;
  }).join("\n");

  const promptText = `
    Actúa como un chef experto. El usuario quiere saber qué cocinar.

    Inventario del usuario:
    ${inventoryList}

    Pregunta del usuario: "${userQuestion}"

    Instrucciones:
    1. Recomienda una receta usando los ingredientes del inventario.
    2. Prioriza ingredientes próximos a caducar.
    3. Si faltan ingredientes esenciales, menciónalos.
    4. Sé breve y amigable.
  `;

  // 3. Generar respuesta usando la instancia 'ai'
  const llmResponse = await ai.generate({
    prompt: promptText,
    config: {
      temperature: 0.7,
    },
  });

  // 4. Usar la propiedad .text (sin paréntesis en la nueva versión)
  return {text: llmResponse.text};
});