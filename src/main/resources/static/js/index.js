document.addEventListener("DOMContentLoaded", function () {

    // --- 1. Highlight failing grades on report tables ---
    const gradeCells = document.querySelectorAll("td");
    gradeCells.forEach(cell => {
        const value = parseFloat(cell.textContent);
        if (!isNaN(value) && value < 10) {  // failing grade threshold
            cell.style.color = "red";
            cell.style.fontWeight = "bold";
        }
    });

    // --- 2. Confirm deletion links ---
    const deleteLinks = document.querySelectorAll("a.delete-link");
    deleteLinks.forEach(link => {
        link.addEventListener("click", function (e) {
            const name = this.getAttribute("data-name");
            if (!confirm("Are you sure you want to delete " + name + "?")) {
                e.preventDefault();
            }
        });
    });

    // --- 3. Dynamic calculation of term average in assessment form ---
    const scoreInputs = document.querySelectorAll("input[name^='score_']");
    const avgDisplay = document.getElementById("term-average");

    if (scoreInputs.length && avgDisplay) {
        function calculateAverage() {
            let total = 0;
            let count = 0;
            scoreInputs.forEach(input => {
                const val = parseFloat(input.value);
                if (!isNaN(val)) {
                    total += val;
                    count++;
                }
            });
            const avg = count > 0 ? (total / count).toFixed(2) : 0;
            avgDisplay.textContent = avg;
        }

        scoreInputs.forEach(input => {
            input.addEventListener("input", calculateAverage);
        });
    }
});
