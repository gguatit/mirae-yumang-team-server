/**
 * timetable-fetcher.mjs
 * comcigan-parser (patched local copy)를 사용하여 근명고등학교 시간표 데이터를 가져옵니다.
 * JSON 형태로 stdout에 출력합니다.
 */
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const Timetable = require('./comcigan-parser-local/index.js');

const SCHOOL_NAME = '근명고등학교';

async function main() {
    const timetable = new Timetable();
    await timetable.init({ maxGrade: 3, cache: 1000 * 60 * 60 });

    const schools = await timetable.search(SCHOOL_NAME);
    const target = schools.find(s => s.name === SCHOOL_NAME);

    if (!target) {
        console.error('SCHOOL_NOT_FOUND');
        process.exit(1);
    }

    timetable.setSchool(target.code);

    const [classTimes, tableData] = await Promise.all([
        timetable.getClassTime(),
        timetable.getTimetable()
    ]);

    console.log(JSON.stringify({ classTimes, timetable: tableData }));
}

main().catch(err => {
    console.error('FETCH_ERROR:', err.message);
    process.exit(1);
});
