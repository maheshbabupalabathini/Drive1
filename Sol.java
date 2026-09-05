const fs = require("fs");

// Read JSON input from input.json
const data = JSON.parse(fs.readFileSync("input.json", "utf8"));

const n = data.keys.n;
const k = data.keys.k;

// --------------------------------------------------
// Convert a value from any base (2 to 36) to BigInt
// --------------------------------------------------
function convertToDecimal(value, base) {
    const digits = "0123456789abcdefghijklmnopqrstuvwxyz";
    let result = 0n;
    const b = BigInt(base);

    value = value.toLowerCase();

    for (const ch of value) {
        const digit = digits.indexOf(ch);

        if (digit < 0 || digit >= base) {
            throw new Error(
                `Invalid digit '${ch}' for base ${base}`
            );
        }

        result = result * b + BigInt(digit);
    }

    return result;
}

// --------------------------------------------------
// Read and decode all points
// Each JSON key is x
// value is y encoded in the given base
// --------------------------------------------------
const points = [];

for (let i = 1; i <= n; i++) {
    const item = data[String(i)];

    if (!item) {
        continue;
    }

    const x = BigInt(i);
    const base = parseInt(item.base, 10);
    const y = convertToDecimal(item.value, base);

    points.push({ x, y });
}

console.log("Decoded points:");

for (const point of points) {
    console.log(`(${point.x}, ${point.y})`);
}

// --------------------------------------------------
// Lagrange interpolation at x = 0
//
// f(0) = Σ yi * Π [ (0 - xj) / (xi - xj) ]
//
// We use exact rational arithmetic using BigInt.
// --------------------------------------------------

function gcd(a, b) {
    a = a < 0n ? -a : a;
    b = b < 0n ? -b : b;

    while (b !== 0n) {
        const temp = a % b;
        a = b;
        b = temp;
    }

    return a;
}

function normalize(num, den) {
    if (den < 0n) {
        num = -num;
        den = -den;
    }

    const g = gcd(num, den);

    return {
        num: num / g,
        den: den / g
    };
}

function addFractions(a, b) {
    return normalize(
        a.num * b.den + b.num * a.den,
        a.den * b.den
    );
}

function multiplyFraction(a, b) {
    return normalize(
        a.num * b.num,
        a.den * b.den
    );
}

function lagrangeAtZero(selectedPoints) {

    let result = {
        num: 0n,
        den: 1n
    };

    for (let i = 0; i < selectedPoints.length; i++) {

        const xi = selectedPoints[i].x;
        const yi = selectedPoints[i].y;

        let basis = {
            num: 1n,
            den: 1n
        };

        for (let j = 0; j < selectedPoints.length; j++) {

            if (i === j) {
                continue;
            }

            const xj = selectedPoints[j].x;

            // (0 - xj) / (xi - xj)
            const factor = normalize(
                -xj,
                xi - xj
            );

            basis = multiplyFraction(basis, factor);
        }

        // yi * basis
        const term = multiplyFraction(
            {
                num: yi,
                den: 1n
            },
            basis
        );

        result = addFractions(result, term);
    }

    return result;
}

// --------------------------------------------------
// k = m + 1
// Therefore polynomial degree = k - 1
// --------------------------------------------------

const degree = k - 1;

console.log(`\nPolynomial degree: ${degree}`);
console.log(`Required points (k): ${k}`);

// Use the first k points
const selectedPoints = points.slice(0, k);

const result = lagrangeAtZero(selectedPoints);

console.log("\nConstant term f(0):");

if (result.den === 1n) {
    console.log(result.num.toString());
} else {
    console.log(`${result.num}/${result.den}`);
}